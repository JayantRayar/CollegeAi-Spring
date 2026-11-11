
import os
from dotenv import load_dotenv
from groq import Groq

# Load environment variables from .env
load_dotenv()

# Initialize Groq client
client = Groq(api_key=os.getenv("GROQ_API_KEY"))


def transcribe_audio(audio_path):
    """
    Convert an audio file into English text using Groq Whisper.
    """

    with open(audio_path, "rb") as audio_file:
        transcription = client.audio.transcriptions.create(
            file=(os.path.basename(audio_path), audio_file.read()),
            model="whisper-large-v3-turbo",
            language="en",
            response_format="text"
        )

    return transcription
