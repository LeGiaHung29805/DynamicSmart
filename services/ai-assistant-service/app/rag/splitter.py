from __future__ import annotations

import hashlib

from app.rag.types import Chunk, Document


class RecursiveTextSplitter:
    """Small recursive character splitter following the structure in the video."""

    def __init__(self, chunk_size: int, chunk_overlap: int):
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.separators = ("\n\n", "\n", ". ", "; ", ", ", " ")

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

        pieces = self._recursive_split(normalized, 0)
        chunks: list[str] = []
        current = ""
        for piece in pieces:
            candidate = f"{current} {piece}".strip() if current else piece.strip()
            if current and len(candidate) > self.chunk_size:
                chunks.append(current)
                overlap = current[-self.chunk_overlap :] if self.chunk_overlap else ""
                current = f"{overlap} {piece}".strip()
                if len(current) > self.chunk_size:
                    current = current[-self.chunk_size :]
            else:
                current = candidate
        if current:
            chunks.append(current)
        return chunks

    def _recursive_split(self, text: str, separator_index: int) -> list[str]:
        if len(text) <= self.chunk_size:
            return [text]
        if separator_index >= len(self.separators):
            return [text[index : index + self.chunk_size] for index in range(0, len(text), self.chunk_size)]

        separator = self.separators[separator_index]
        pieces = text.split(separator)
        if len(pieces) == 1:
            return self._recursive_split(text, separator_index + 1)

        result: list[str] = []
        for piece in pieces:
            piece = piece.strip()
            if not piece:
                continue
            result.extend(self._recursive_split(piece, separator_index + 1))
        return result

