from dotenv import load_dotenv
from groq import Groq
import os

# Load environment variables from .env
load_dotenv()

# Create Groq client using API key
client = Groq(api_key=os.getenv("GROQ_API_KEY"))

# Check connection by listing available models
models = client.models.list()

print("Groq API connection successful!")