import os
import struct
import logging
from dotenv import load_dotenv
from google import genai
from google.genai import types

load_dotenv()

logger = logging.getLogger(__name__)

SAMPLE_RATE = 24000   # Gemini TTS outputs 24 kHz PCM
CHANNELS = 1          # Mono
BITS_PER_SAMPLE = 16  # 16-bit PCM


def _build_wav_header(pcm_data: bytes) -> bytes:
    """
    Constructs a valid 44-byte RIFF WAV header for the given raw PCM data.

    Gemini TTS returns raw 16-bit signed little-endian PCM at 24 kHz mono.
    Without a WAV header the browser cannot decode the audio.
    """
    num_channels = CHANNELS
    sample_rate = SAMPLE_RATE
    bits_per_sample = BITS_PER_SAMPLE
    byte_rate = sample_rate * num_channels * bits_per_sample // 8
    block_align = num_channels * bits_per_sample // 8
    data_size = len(pcm_data)
    chunk_size = 36 + data_size  # 4 (RIFF) + 4 (size) + 4 (WAVE) + 24 (fmt) + 8 (data hdr)

    header = struct.pack(
        "<4sI4s4sIHHIIHH4sI",
        b"RIFF",         # ChunkID
        chunk_size,      # ChunkSize
        b"WAVE",         # Format
        b"fmt ",         # Subchunk1ID
        16,              # Subchunk1Size (PCM)
        1,               # AudioFormat (1 = PCM)
        num_channels,    # NumChannels
        sample_rate,     # SampleRate
        byte_rate,       # ByteRate
        block_align,     # BlockAlign
        bits_per_sample, # BitsPerSample
        b"data",         # Subchunk2ID
        data_size        # Subchunk2Size
    )
    return header


def _get_gemini_client() -> genai.Client:
    """Returns a configured Gemini client. Raises at import time if key is missing."""
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise EnvironmentError(
            "GEMINI_API_KEY environment variable is not set."
        )
    return genai.Client(api_key=api_key)


# Initialise once at module import — raises immediately if key is absent.
_client = _get_gemini_client()


def generate_speech(text: str) -> bytes:
    """
    Converts text to speech using Gemini TTS and returns a valid WAV file.

    Gemini TTS returns raw 16-bit 24 kHz mono PCM.
    This function prepends the standard RIFF WAV header so browsers can play it.

    Args:
        text: The text to convert to speech.

    Returns:
        A valid WAV file as bytes (44-byte header + PCM data).

    Raises:
        RuntimeError: If Gemini TTS returns no audio data.
    """
    logger.info("Generating TTS audio for text of length %d", len(text))

    response = _client.models.generate_content(
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
            if part.inline_data and part.inline_data.data:
                pcm_data = part.inline_data.data
                wav_header = _build_wav_header(pcm_data)
                logger.info(
                    "TTS audio generated: %d bytes PCM → %d bytes WAV",
                    len(pcm_data),
                    len(wav_header) + len(pcm_data)
                )
                return wav_header + pcm_data

    raise RuntimeError(
        "Gemini TTS returned no audio data. "
        "The model may be unavailable or the input was rejected."
    )
