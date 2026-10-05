from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Document:
    tenant: str
    source: str
    title: str
    content: str


@dataclass(frozen=True)
class Chunk:
    tenant: str
    chunk_id: str
    source: str
    title: str
    content: str


@dataclass(frozen=True)
class SearchResult:
    chunk: Chunk
    score: float

