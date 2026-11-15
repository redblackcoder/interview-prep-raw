# Boolean document search

Search an in-memory document collection with nested AND and OR expressions and ranked partial matches.

## Worked solution

Tokenize each document into words and store a map from word to document ID and frequency. Parse parentheses as subexpressions and give AND precedence over OR. At a leaf, union postings for vocabulary words containing the query term. Intersect sets at AND nodes and union at OR nodes.

For each eligible document sum term-frequency times inverse document frequency across matching terms, weighting an exact word by 1 and a substring by 0.5. Break score ties by document ID. On replacement, remove the old document postings before indexing the new text.

For (red OR blue) AND boat, red alone cannot admit a document. Both red boat and blue sailboat qualify, with a larger boat contribution for a whole-word match.

## Reference implementation

Reconstructed Python example.

```python
from collections import Counter, defaultdict
from math import log
import re


class Index:
    def __init__(self):
        self.words = {}
        self.postings = defaultdict(dict)

    def add(self, doc_id, text):
        for word in self.words.get(doc_id, ()):
            del self.postings[word][doc_id]
            if not self.postings[word]:
                del self.postings[word]
        counts = Counter(re.findall(r"\w+", text.casefold()))
        self.words[doc_id] = counts
        for word, count in counts.items():
            self.postings[word][doc_id] = count

    def search(self, query):
        tokens = iter(re.findall(r"\w+|[^\s\w]", query))
        current = next(tokens, None)
        scores = Counter()

        def advance():
            nonlocal current
            token = current
            current = next(tokens, None)
            return token

        def primary():
            token = advance()
            if token == "(":
                docs = union()
                if advance() != ")":
                    raise ValueError("missing closing parenthesis")
                return docs
            if token is None or not re.fullmatch(r"\w+", token) or token in {"AND", "OR"}:
                raise ValueError("expected term")
            term = token.casefold()
            docs = set()
            for word, postings in self.postings.items():
                if term in word:
                    weight = 1 if term == word else 0.5
                    idf = 1 + log((1 + len(self.words)) / (1 + len(postings)))
                    docs.update(postings)
                    for doc, tf in postings.items():
                        scores[doc] += weight * tf * idf
            return docs

        def intersection():
            docs = primary()
            while current == "AND":
                advance()
                docs &= primary()
            return docs

        def union():
            docs = intersection()
            while current == "OR":
                advance()
                docs |= intersection()
            return docs

        matches = union()
        if current is not None:
            raise ValueError("unexpected token")
        return sorted(matches, key=lambda doc: (-scores[doc], doc))


index = Index()
index.add("a", "red boat")
index.add("b", "blue sailboat")
index.add("c", "red")
assert set(index.search("(red OR blue) AND boat")) == {"a", "b"}
index.add("a", "green")
assert index.search("red AND boat") == []
```
