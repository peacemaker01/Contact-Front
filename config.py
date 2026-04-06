import os
from dotenv import load_dotenv

load_dotenv()

# Read from .env
api_key = os.environ.get("LLM_API_KEY") or os.environ.get("OPENROUTER_API_KEY")
provider = os.environ.get("LLM_PROVIDER", "openrouter").lower()

# Configure OpenRouter (default)
OPENROUTER_CONFIG = {
    "base_url": "https://openrouter.ai/api/v1/chat/completions",
    "api_key": api_key if provider == "openrouter" else None,
    "model": "mistralai/mixtral-8x7b-instruct",
    "fallback_model": "meta-llama/llama-3-8b-instruct",
    "headers": {
        "HTTP-Referer": "https://contact-front.local",
        "X-Title": "Contact Front Wargame"
    },
    "temperature": 0.4,
    "max_tokens": 800,
    "timeout": 15
}

# If using DeepSeek, override config (optional)
if provider == "deepseek" and api_key:
    OPENROUTER_CONFIG["base_url"] = "https://api.deepseek.com/v1/chat/completions"
    OPENROUTER_CONFIG["model"] = "deepseek-chat"
    OPENROUTER_CONFIG["headers"] = {}

# If using Claude (OpenRouter still works, but we can set specific model)
if provider == "claude" and api_key:
    OPENROUTER_CONFIG["model"] = "anthropic/claude-3-haiku-20240307"

GAME_NAME = "CONTACT FRONT"
VERSION = "1.0"
