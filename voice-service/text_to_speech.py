import os
from dotenv import load_dotenv
from google import genai
from google.genai import types

load_dotenv()

client = genai.Client(
    api_key=os.getenv("GEMINI_API_KEY")
)


def generate_speech(text: str) -> bytes:
    response = client.models.generate_content(
        model="gemini-3.8-flash-lite-tts",
        contents=text,
        config=types.GenerateContentConfig(
            response_modalities=["AUDIO"],
            speech_config=types.SpeechConfig(
                voice_config=types.VoiceConfig(
                    prebuilt_voice_config=types.PrebuiltVoiceConfig(
                        voice_name="Kore"
                    )
                )
            )
        )
    )

    if response.parts:
        for part in response.parts:
            if part.inline_data:
                return part.inline_data.data

    raise RuntimeError("Gemini TTS returned no audio")