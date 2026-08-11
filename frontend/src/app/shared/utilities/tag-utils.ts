export const MAX_TAGS = 50;
export const TAG_PATTERN = /^[A-Za-z0-9_-]{1,80}$/;

export function normalizeTags(tags: string[]): string[] {
  const seen = new Set<string>();
  const normalized: string[] = [];
  tags.forEach((tag) => {
    const value = tag.trim().toLowerCase();
    if (value && !seen.has(value)) {
      seen.add(value);
      normalized.push(value);
    }
  });
  return normalized.sort();
}

export function invalidTags(tags: string[]): string[] {
  return normalizeTags(tags).filter((tag) => !TAG_PATTERN.test(tag));
}
