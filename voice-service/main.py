
from fastapi import FastAPI, UploadFile, File, HTTPException, Response
from speech_to_text import transcribe_audio
from text_to_speech import generate_speech
import shutil
import os
import uuid
import io
import wave
import audioop

app = FastAPI(title="College AI Voice Service")


@app.get("/")
def home():
    return {"message": "College AI Voice Service is running"}


# Speech-to-Text using Groq Whisper
@app.post("/api/speech-to-text")
async def speech_to_text(file: UploadFile = File(...)):
    temp_file = f"temp_{uuid.uuid4().hex}_{file.filename}"

    try:
        with open(temp_file, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        text = transcribe_audio(temp_file)

        return {
            "success": True,
            "transcribedText": text
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    finally:
        if os.path.exists(temp_file):
            os.remove(temp_file)

        await file.close()


# Text-to-Speech using Gemini
@app.post("/api/text-to-speech")
async def text_to_speech(request: dict):
    text = request.get("text", "").strip()

    if not text:
        raise HTTPException(
            status_code=400,
            detail="Text is required"
        )

    try:
        audio_bytes = generate_speech(text)

        return Response(
            content=audio_bytes,
            media_type="audio/wav"
        )

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=str(e)
        )

    # Exotel-compatible Text-to-Speech (8 kHz raw PCM)
@app.post("/api/text-to-speech/exotel")
async def text_to_speech_exotel(request: dict):
    text = request.get("text", "").strip()

    if not text:
        raise HTTPException(status_code=400, detail="Text is required")

    try:
        audio_bytes = generate_speech(text)

        with wave.open(io.BytesIO(audio_bytes), "rb") as wav:
            pcm_data = wav.readframes(wav.getnframes())
            sample_rate = wav.getframerate()
            channels = wav.getnchannels()
            sample_width = wav.getsampwidth()

        if channels != 1 or sample_width != 2:
            raise ValueError("Expected mono, 16-bit PCM audio")

        pcm_8khz, _ = audioop.ratecv(
            pcm_data, 2, 1, sample_rate, 8000, None
        )

        return Response(
            content=pcm_8khz,
            media_type="application/octet-stream"
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
