from __future__ import annotations

from pathlib import Path

from pypdf import PdfReader

from app.rag.types import Document


class DirectoryDocumentLoader:
    """Equivalent to the DirectoryLoader stage used in the reference video."""

    SUPPORTED_SUFFIXES = {".md", ".txt", ".pdf"}

    def __init__(self, root: Path):
        self.root = root

    def load(self) -> list[Document]:
        if not self.root.exists():
            return []

        documents: list[Document] = []
        for tenant_dir in sorted(path for path in self.root.iterdir() if path.is_dir()):
            for path in sorted(tenant_dir.rglob("*")):
                if not path.is_file() or path.suffix.lower() not in self.SUPPORTED_SUFFIXES:
                    continue
                content = self._read(path).strip()
                if not content:
                    continue
                documents.append(
                    Document(
                        tenant=tenant_dir.name,
                        source=path.relative_to(self.root).as_posix(),
                        title=self._title(path, content),
                        content=content,
                    )
                )
        return documents

    def _read(self, path: Path) -> str:
        if path.suffix.lower() == ".pdf":
            return "\n\n".join((page.extract_text() or "") for page in PdfReader(path).pages)
        return path.read_text(encoding="utf-8")

    @staticmethod
    def _title(path: Path, content: str) -> str:
        for line in content.splitlines():
            candidate = line.lstrip("# ").strip()
            if candidate:
                return candidate[:180]
        return path.stem.replace("-", " ").title()

