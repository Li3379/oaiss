"""
OAISS CHAIN ML Service - FastAPI Application Entry Point

Provides prediction endpoints for carbon emission forecasting,
enterprise inference, and market forecasting.
"""

import logging
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI

from app.routers.enterprise_router import router as enterprise_router
from app.routers.market_router import router as market_router
from app.schemas.emission import EmissionForecastRequest, EmissionForecastResponse
from app.schemas.market import MarketForecastRequest, MarketForecastResponse

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(application: FastAPI) -> AsyncGenerator[None, None]:
    """Application lifespan: startup and shutdown hooks."""
    logger.info("Starting ML service")
    yield
    logger.info("Shutting down ML service")


app = FastAPI(
    title="OAISS Chain ML Service",
    version="1.0.0",
    lifespan=lifespan,
)

# Register market prediction router (Plan 07-02)
app.include_router(market_router)

# Register enterprise inference router (Plan 07-03)
app.include_router(enterprise_router)


@app.get("/health")
async def health_check() -> dict[str, str]:
    """Health check endpoint for Docker and load balancer probes."""
    return {"status": "healthy", "version": "1.0.0"}


@app.post(
    "/api/v1/predict/emission",
    response_model=EmissionForecastResponse,
    summary="Emission forecast (stub)",
)
async def predict_emission(request: EmissionForecastRequest) -> None:
    """Stub endpoint for emission forecasting. Returns 501 until Plan 07-04 implements it."""
    from fastapi.responses import JSONResponse

    return JSONResponse(
        status_code=501,
        content={"detail": "Emission prediction endpoint not yet implemented"},
    )
