"""Shared-secret validation for backend-to-ML service calls."""

from fastapi import Header, HTTPException, status

from app.config import settings


def require_ml_service_secret(
    x_ml_service_secret: str | None = Header(default=None, alias="X-ML-Service-Secret"),
) -> None:
    """Require matching shared secret when ML_SERVICE_SECRET is configured."""
    configured_secret = settings.ml_service_secret.strip()
    if not configured_secret:
        return

    if x_ml_service_secret != configured_secret:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid ML service secret",
        )
