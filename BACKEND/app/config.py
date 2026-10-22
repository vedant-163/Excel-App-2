from functools import lru_cache
from pathlib import Path
from urllib.parse import quote_plus

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


BASE_DIR = Path(__file__).resolve().parent.parent
ENV_FILE = BASE_DIR / ".env"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(ENV_FILE),
        env_file_encoding="utf-8",
        env_prefix="",
        case_sensitive=False,
        extra="ignore",
    )

    db_host: str = "127.0.0.1"
    db_port: int = 3306
    db_name: str = "excel_app"
    db_user: str = "root"
    db_password: str = ""

    jwt_secret: str = Field(min_length=32)
    jwt_expire_minutes: int = Field(default=480, ge=5, le=1440)

    cors_origins: str = "*"

    # Optional email delivery for staff password recovery. Keep these only in local .env.
    smtp_host: str = ""
    smtp_port: int = Field(default=587, ge=1, le=65535)
    smtp_username: str = ""
    smtp_password: str = ""
    smtp_from_email: str = ""
    smtp_use_tls: bool = True
    password_reset_expiry_minutes: int = Field(default=15, ge=5, le=60)

    @property
    def database_url(self) -> str:
        encoded_pw = quote_plus(self.db_password)

        return (
            f"mysql+pymysql://{self.db_user}:{encoded_pw}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
            "?charset=utf8mb4"
        )

    @property
    def cors_origin_list(self) -> list[str]:
        if self.cors_origins.strip() == "*":
            return ["*"]

        return [
            item.strip()
            for item in self.cors_origins.split(",")
            if item.strip()
        ]


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()