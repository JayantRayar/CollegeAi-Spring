import os
import logging
from dotenv import load_dotenv
from groq import Groq

load_dotenv()

logger = logging.getLogger(__name__)

# Allowed audio MIME types accepted by Groq Whisper.
# WebM/Opus is the default format produced by the browser MediaRecorder API.
ALLOWED_AUDIO_MIME_TYPES = {
    "audio/webm",
    "audio/webm;codecs=opus",
    "audio/wav",
    "audio/wave",
    "audio/x-wav",
    "audio/mp4",
    "audio/mpeg",
    "audio/mp3",
    "audio/ogg",
    "audio/flac",
    "audio/x-flac",
    "audio/m4a",
    "audio/x-m4a",
    "video/webm",   # Chrome sometimes reports WebM audio as video/webm
}

# Maximum audio file size accepted (25 MB — Groq Whisper's API limit)
MAX_AUDIO_SIZE_BYTES = 25 * 1024 * 1024


def _get_groq_client() -> Groq:
    """Returns a configured Groq client. Raises at import time if key is missing."""
    api_key = os.getenv("GROQ_API_KEY")
    if not api_key:
        raise EnvironmentError(
            "GROQ_API_KEY environment variable is not set."
        )
    return Groq(api_key=api_key)


# Initialise once at module import — raises immediately if key is absent.
_client = _get_groq_client()


def transcribe_audio(audio_path: str) -> str:
    """
    Converts an audio file to English text using Groq Whisper.

    Args:
        audio_path: Absolute path to the audio file on disk.

    Returns:
        Transcribed text string. Never returns an empty string
        without raising; callers should check for blank output.

    Raises:
        RuntimeError: If Groq Whisper returns no transcription.
    """
    logger.info("Transcribing audio file: %s", os.path.basename(audio_path))

    with open(audio_path, "rb") as audio_file:
        transcription = _client.audio.transcriptions.create(
            # Pass only the basename — the tuple form is (filename, bytes).
            # Using only uuid-based temp name avoids leaking original filename.
            file=(os.path.basename(audio_path), audio_file.read()),
            model="whisper-large-v3-turbo",
            language="en",
            response_format="text"
        )

    if not transcription or not str(transcription).strip():
        raise RuntimeError(
            "Whisper returned an empty transcription. "
            "The audio may be silent, too short, or inaudible."
        )

    result = str(transcription).strip()
    logger.info("Transcription completed (%d characters).", len(result))
    return result
