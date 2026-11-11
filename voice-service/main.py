
from fastapi import FastAPI, UploadFile, File, HTTPException, Response
from speech_to_text import transcribe_audio
from text_to_speech import generate_speech
import shutil
import os
import uuid

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
