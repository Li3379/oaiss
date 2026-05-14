"""
Market forecast request/response schemas.
"""

from pydantic import BaseModel, Field


class MarketForecastRequest(BaseModel):
    """Request schema for carbon market price forecasting."""

    dates: list[str] = Field(..., min_length=1, description="ISO 8601 date strings")
    prices: list[float] = Field(..., min_length=1, description="Historical prices")
    volumes: list[float] = Field(..., min_length=1, description="Historical volumes")
    horizon_days: int = Field(
        default=30, ge=1, le=365, description="Forecast horizon in days"
    )


class MarketForecastResponse(BaseModel):
    """Response schema for carbon market price forecasting."""

    forecast_dates: list[str]
    forecast_prices: list[float]
    lower_bound: list[float]
    upper_bound: list[float]
    trend: str  # "up" | "stable" | "down"
    model_version: str = "1.0.0"
