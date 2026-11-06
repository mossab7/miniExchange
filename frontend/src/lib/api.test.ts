import { describe, expect, it } from 'vitest';
import { errorMessage } from './api';

describe('API error handling', () => {
  it('prefers the backend message when one is returned', () => {
    expect(errorMessage({ response: { data: { message: 'Insufficient balance' } } })).toBe('Insufficient balance');
  });

  it('returns a safe fallback for unknown failures', () => {
    expect(errorMessage(new Error('network'))).toBe('The backend could not complete that request.');
  });

  it('explains when the API cannot be reached', () => {
    expect(errorMessage({ request: {} })).toContain('backend is unavailable');
  });
});
