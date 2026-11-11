from dotenv import load_dotenv
from groq import Groq
import os

# Load API key from .env
load_dotenv()

# Initialize Groq client
client = Groq(api_key=os.getenv("GROQ_API_KEY"))

# Open English audio file
with open("samples/test.wav", "rb") as audio_file:

    # Convert speech into text using Whisper
    transcription = client.audio.transcriptions.create(
        file=("test.wav", audio_file.read()),
        model="whisper-large-v3-turbo",
        language="en",
        response_format="text"
    )

# Display transcribed text
print("\nTranscribed Text:")
print(transcription)