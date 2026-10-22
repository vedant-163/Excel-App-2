from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy.exc import IntegrityError, OperationalError, SQLAlchemyError


class AppError(HTTPException):
    def __init__(self, status_code: int, message: str, details: dict | None = None):
        super().__init__(status_code=status_code, detail=message)
        self.message = message
        self.details = details or {}


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppError)
    async def app_error_handler(_request: Request, exc: AppError) -> JSONResponse:
        body: dict = {"success": False, "message": exc.message, "data": None}
        if exc.details:
            body["errors"] = exc.details
        return JSONResponse(status_code=exc.status_code, content=body)

    @app.exception_handler(RequestValidationError)
    async def validation_handler(
        _request: Request, exc: RequestValidationError
    ) -> JSONResponse:
        errors = []
        for item in exc.errors():
            loc = ".".join(str(part) for part in item.get("loc", []) if part != "body")
            errors.append({"field": loc, "message": item.get("msg")})
        return JSONResponse(
            status_code=422,
            content={
                "success": False,
                "message": "Validation failed",
                "data": None,
                "errors": errors,
            },
        )

    @app.exception_handler(IntegrityError)
    async def integrity_handler(_request: Request, _exc: IntegrityError) -> JSONResponse:
        return JSONResponse(
            status_code=409,
            content={
                "success": False,
                "message": "The record conflicts with an existing unique value",
                "data": None,
            },
        )

    @app.exception_handler(OperationalError)
    async def operational_handler(
        _request: Request, _exc: OperationalError
    ) -> JSONResponse:
        return JSONResponse(
            status_code=503,
            content={
                "success": False,
                "message": "Database is unavailable. Try again later.",
                "data": None,
            },
        )

    @app.exception_handler(SQLAlchemyError)
    async def sql_handler(_request: Request, _exc: SQLAlchemyError) -> JSONResponse:
        return JSONResponse(
            status_code=500,
            content={
                "success": False,
                "message": "A database error occurred",
                "data": None,
            },
        )
