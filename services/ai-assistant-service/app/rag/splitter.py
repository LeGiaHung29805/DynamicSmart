from __future__ import annotations

import hashlib
import re

from app.rag.types import Chunk, Document


class RecursiveTextSplitter:
    """Small recursive character splitter following the structure in the video."""

    def __init__(self, chunk_size: int, chunk_overlap: int):
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap

    def split_documents(self, documents: list[Document]) -> list[Chunk]:
        chunks: list[Chunk] = []
        for document in documents:
            for index, content in enumerate(self._split(document.content)):
                digest = hashlib.sha256(
                    f"{document.tenant}|{document.source}|{index}|{content}".encode("utf-8")
                ).hexdigest()
                chunks.append(
                    Chunk(
                        tenant=document.tenant,
                        chunk_id=digest,
                        source=document.source,
                        title=document.title,
                        content=content,
                    )
                )
        return chunks

    def _split(self, text: str) -> list[str]:
        normalized = "\n".join(line.rstrip() for line in text.splitlines()).strip()
        if len(normalized) <= self.chunk_size:
            return [normalized] if normalized else []

        pieces = self._semantic_units(normalized)
        chunks: list[str] = []
        current: list[str] = []
        for piece in pieces:
            candidate = self._join(current + [piece])
            if current and len(candidate) > self.chunk_size:
                chunks.append(self._join(current))
                current = self._whole_unit_overlap(current)
                while current and len(self._join(current + [piece])) > self.chunk_size:
                    current.pop(0)
                current.append(piece)
            else:
                current.append(piece)
        if current:
            chunks.append(self._join(current))
        return chunks

    def _semantic_units(self, text: str) -> list[str]:
        units: list[str] = []
        for paragraph in re.split(r"\n\s*\n", text):
            paragraph = " ".join(part.strip() for part in paragraph.splitlines() if part.strip())
            if not paragraph:
                continue
            if len(paragraph) <= self.chunk_size:
                units.append(paragraph)
                continue
            sentences = re.split(r"(?<=[.!?])\s+", paragraph)
            for sentence in sentences:
                sentence = sentence.strip()
                if not sentence:
                    continue
                units.extend(self._split_long_unit(sentence))
        return units

    def _split_long_unit(self, text: str) -> list[str]:
        if len(text) <= self.chunk_size:
            return [text]
        words = text.split()
        if len(words) == 1:
            return [text[index : index + self.chunk_size] for index in range(0, len(text), self.chunk_size)]
        units: list[str] = []
        current: list[str] = []
        for word in words:
            candidate = " ".join(current + [word])
            if current and len(candidate) > self.chunk_size:
                units.append(" ".join(current))
                current = [word]
            else:
                current.append(word)
        if current:
            units.append(" ".join(current))
        return units

    def _whole_unit_overlap(self, units: list[str]) -> list[str]:
        if not self.chunk_overlap:
            return []
        overlap: list[str] = []
        for unit in reversed(units):
            candidate = self._join([unit] + overlap)
            if overlap and len(candidate) > self.chunk_overlap:
                break
            overlap.insert(0, unit)
            if len(candidate) >= self.chunk_overlap:
                break
        return overlap

    @staticmethod
    def _join(units: list[str]) -> str:
        return "\n\n".join(units).strip()
