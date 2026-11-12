"""
College AI Voice Service
========================
FastAPI service providing:
  - POST /api/speech-to-text  — Groq Whisper speech-to-text
  - POST /api/text-to-speech  — Gemini TTS text-to-speech
  - GET  /health              — Health check
  - GET  /                    — Service info

Security notes:
  - API keys are read from environment variables only.
  - Uploaded audio files are written to OS-managed temp files; user-supplied
    filenames are never used in filesystem paths.
  - Temporary files are always removed in finally blocks.
  - Internal exception details are never exposed to clients.
  - File size is enforced before writing to disk.
  - MIME type is validated before passing to Groq.
"""

import logging
import os
import sys
import tempfile

from fastapi import FastAPI, HTTPException, Request, UploadFile, File, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, Response
from pydantic import BaseModel, field_validator

from speech_to_text import ALLOWED_AUDIO_MIME_TYPES, MAX_AUDIO_SIZE_BYTES, transcribe_audio
from text_to_speech import generate_speech

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Startup validation — fail fast if required environment variables are missing.
# ---------------------------------------------------------------------------

_REQUIRED_ENV_VARS = ["GROQ_API_KEY", "GEMINI_API_KEY"]

for _var in _REQUIRED_ENV_VARS:
    if not os.getenv(_var):
        logger.critical(
            "Required environment variable '%s' is not set. "
            "The voice service cannot start without it.",
            _var,
        )
        sys.exit(1)

# ---------------------------------------------------------------------------
# FastAPI application
# ---------------------------------------------------------------------------

app = FastAPI(
    title="College AI Voice Service",
    description=(
        "Voice service for the College AI Assistant. "
        "Provides speech-to-text (Groq Whisper) and text-to-speech (Gemini TTS)."
    ),
    version="1.0.0",
)

# ---------------------------------------------------------------------------
# CORS — allow only the configured frontend origin.
# ---------------------------------------------------------------------------

_frontend_origin = os.getenv("FRONTEND_ORIGIN", "http://localhost:5173")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[_frontend_origin],
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)

# ---------------------------------------------------------------------------
# Request / response models
# ---------------------------------------------------------------------------


class TextToSpeechRequest(BaseModel):
    text: str

    @field_validator("text")
    @classmethod
    def text_must_not_be_blank(cls, value: str) -> str:
        stripped = value.strip()
        if not stripped:
            raise ValueError("text must not be empty or blank")
        if len(stripped) > 5000:
            raise ValueError("text must not exceed 5000 characters")
        return stripped


class TranscriptionResponse(BaseModel):
    success: bool
    transcribedText: str


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str


# ---------------------------------------------------------------------------
# Exception handlers
# ---------------------------------------------------------------------------


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    logger.error(
        "Unhandled exception on %s %s",
        request.method,
        request.url.path,
        exc_info=exc,
    )
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"detail": "An unexpected error occurred. Please try again later."},
    )


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------


@app.get("/", include_in_schema=False)
def root() -> dict:
    return {
        "service": "College AI Voice Service",
        "version": "1.0.0",
        "status": "running",
    }


@app.get("/health", response_model=HealthResponse)
def health_check() -> HealthResponse:
    """
    Health check endpoint.
    Returns 200 when the service is running and all required env vars are set.
    """
    return HealthResponse(
        status="healthy",
        service="college-ai-voice-service",
        version="1.0.0",
    )


@app.post("/api/speech-to-text", response_model=TranscriptionResponse)
async def speech_to_text(file: UploadFile = File(...)) -> TranscriptionResponse:
    """
    Transcribes uploaded audio to English text using Groq Whisper.

    Accepts: WebM/Opus (browser MediaRecorder default), WAV, MP3, MP4, OGG, FLAC, M4A.
    Max file size: 25 MB.
    """
    # --- Validate MIME type ---
    content_type = (file.content_type or "").strip().lower()

    # Normalise content_type for comparison (strip codec params for base check)
    base_mime = content_type.split(";")[0].strip()

    if base_mime not in ALLOWED_AUDIO_MIME_TYPES and content_type not in ALLOWED_AUDIO_MIME_TYPES:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail=(
                f"Unsupported audio format: '{base_mime}'. "
                "Accepted formats: WebM, WAV, MP3, MP4, OGG, FLAC, M4A."
            ),
        )

    # --- Read audio bytes with size enforcement ---
    try:
        audio_bytes = await file.read(MAX_AUDIO_SIZE_BYTES + 1)
    except Exception:
        logger.error("Failed to read uploaded audio file", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Failed to read the uploaded audio file.",
        )
    finally:
        await file.close()

    if len(audio_bytes) > MAX_AUDIO_SIZE_BYTES:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=(
                f"Audio file exceeds the maximum allowed size of "
                f"{MAX_AUDIO_SIZE_BYTES // (1024 * 1024)} MB."
            ),
        )

    if len(audio_bytes) == 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="The uploaded audio file is empty.",
        )

    # --- Write to OS-managed temp file (safe; user-supplied filename ignored) ---
    # Use a suffix that matches the MIME type so Groq can identify the format.
    suffix = _mime_to_extension(base_mime)
    tmp_fd, tmp_path = tempfile.mkstemp(suffix=suffix, prefix="voice_stt_")

    try:
        with os.fdopen(tmp_fd, "wb") as tmp_file:
            tmp_file.write(audio_bytes)

        # --- Transcribe ---
        try:
            transcribed_text = transcribe_audio(tmp_path)
        except RuntimeError as e:
            # Groq returned no transcription (silent/inaudible audio)
            logger.warning("Groq Whisper returned empty transcription: %s", e)
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail=(
                    "Could not transcribe the audio. "
                    "Please speak clearly and try again."
                ),
            )
        except Exception:
            logger.error("Groq Whisper transcription failed", exc_info=True)
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=(
                    "Speech-to-text service is temporarily unavailable. "
                    "Please try again shortly."
                ),
            )

        return TranscriptionResponse(
            success=True,
            transcribedText=transcribed_text,
        )

    finally:
        # Always clean up the temp file — even if transcription raises.
        try:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)
        except Exception:
            logger.warning("Failed to remove temp file: %s", tmp_path, exc_info=True)


@app.post("/api/text-to-speech")
async def text_to_speech(request: TextToSpeechRequest) -> Response:
    """
    Converts text to speech using Gemini TTS.
    Returns a valid WAV file (RIFF header + 24 kHz 16-bit mono PCM).
    """
    try:
        wav_bytes = generate_speech(request.text)
    except RuntimeError as e:
        logger.warning("Gemini TTS returned no audio: %s", e)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=(
                "Text-to-speech service could not generate audio. "
                "Please try again shortly."
            ),
        )
    except Exception:
        logger.error("Gemini TTS failed unexpectedly", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=(
                "Text-to-speech service is temporarily unavailable. "
                "Please try again shortly."
            ),
        )

    return Response(
        content=wav_bytes,
        media_type="audio/wav",
        headers={
            "Content-Disposition": "inline; filename=response.wav",
            "Cache-Control": "no-store",
        },
    )


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _mime_to_extension(mime: str) -> str:
    """Returns a file extension for the given MIME type."""
    _map = {
        "audio/webm": ".webm",
        "video/webm": ".webm",
        "audio/wav": ".wav",
        "audio/wave": ".wav",
        "audio/x-wav": ".wav",
        "audio/mp4": ".mp4",
        "audio/mpeg": ".mp3",
        "audio/mp3": ".mp3",
        "audio/ogg": ".ogg",
        "audio/flac": ".flac",
        "audio/x-flac": ".flac",
        "audio/m4a": ".m4a",
        "audio/x-m4a": ".m4a",
    }
    return _map.get(mime, ".audio")
