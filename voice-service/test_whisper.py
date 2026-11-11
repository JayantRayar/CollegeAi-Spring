from faster_whisper import WhisperModel

print("Loading Whisper Base model...")

model = WhisperModel(
    "base",
    device="cpu",
    compute_type="int8"
)

print("Model loaded successfully!")

print("Transcribing audio...")

segments, info = model.transcribe(
    "samples/hindi2.wav",
    language="hi",
    beam_size=5
)

print("\nDetected language:", info.language)
print("Language probability:", info.language_probability)

print("\nTranscription:")

for segment in segments:
    print(segment.text)