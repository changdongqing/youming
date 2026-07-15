import { describe, expect, it } from 'vitest';
import {
	isReadonlyStatus,
	isEditableStatus,
	isPublishableStatus,
	isValidatableStatus,
	isRetirableStatus,
	canSave,
	canValidate,
	canPublish,
	canAddEntityMapping,
	canDeleteMapping,
	isRevisionConflict,
	getConflictMessage,
} from './designer-state';

describe('Designer state utilities', () => {
	describe('isReadonlyStatus', () => {
		it('returns true for PUBLISHED', () => {
			expect(isReadonlyStatus('PUBLISHED')).toBe(true);
		});
		it('returns true for VALIDATED', () => {
			expect(isReadonlyStatus('VALIDATED')).toBe(true);
		});
		it('returns true for RETIRED', () => {
			expect(isReadonlyStatus('RETIRED')).toBe(true);
		});
		it('returns true for VALIDATING', () => {
			expect(isReadonlyStatus('VALIDATING')).toBe(true);
		});
		it('returns false for DRAFT', () => {
			expect(isReadonlyStatus('DRAFT')).toBe(false);
		});
	});

	describe('isEditableStatus', () => {
		it('returns true for DRAFT', () => {
			expect(isEditableStatus('DRAFT')).toBe(true);
		});
		it('returns false for PUBLISHED', () => {
			expect(isEditableStatus('PUBLISHED')).toBe(false);
		});
		it('returns false for VALIDATED', () => {
			expect(isEditableStatus('VALIDATED')).toBe(false);
		});
	});

	describe('isPublishableStatus', () => {
		it('returns true for DRAFT', () => {
			expect(isPublishableStatus('DRAFT')).toBe(true);
		});
		it('returns true for VALIDATED', () => {
			expect(isPublishableStatus('VALIDATED')).toBe(true);
		});
		it('returns false for PUBLISHED', () => {
			expect(isPublishableStatus('PUBLISHED')).toBe(false);
		});
		it('returns false for RETIRED', () => {
			expect(isPublishableStatus('RETIRED')).toBe(false);
		});
	});

	describe('isValidatableStatus', () => {
		it('returns true for DRAFT', () => {
			expect(isValidatableStatus('DRAFT')).toBe(true);
		});
		it('returns false for PUBLISHED', () => {
			expect(isValidatableStatus('PUBLISHED')).toBe(false);
		});
		it('returns false for VALIDATED', () => {
			expect(isValidatableStatus('VALIDATED')).toBe(false);
		});
	});

	describe('isRetirableStatus', () => {
		it('returns true for PUBLISHED', () => {
			expect(isRetirableStatus('PUBLISHED')).toBe(true);
		});
		it('returns false for DRAFT', () => {
			expect(isRetirableStatus('DRAFT')).toBe(false);
		});
	});

	describe('canSave', () => {
		it('returns true when DRAFT and dirty', () => {
			expect(canSave('DRAFT', true)).toBe(true);
		});
		it('returns false when DRAFT but not dirty', () => {
			expect(canSave('DRAFT', false)).toBe(false);
		});
		it('returns false when PUBLISHED and dirty', () => {
			expect(canSave('PUBLISHED', true)).toBe(false);
		});
		it('returns false when VALIDATED and dirty', () => {
			expect(canSave('VALIDATED', true)).toBe(false);
		});
	});

	describe('canValidate', () => {
		it('returns true for DRAFT', () => {
			expect(canValidate('DRAFT')).toBe(true);
		});
		it('returns false for PUBLISHED', () => {
			expect(canValidate('PUBLISHED')).toBe(false);
		});
	});

	describe('canPublish', () => {
		it('returns true for VALIDATED without dirty', () => {
			expect(canPublish('VALIDATED', false)).toBe(true);
		});
		it('returns false for VALIDATED with dirty', () => {
			expect(canPublish('VALIDATED', true)).toBe(false);
		});
		it('returns true for DRAFT without dirty', () => {
			expect(canPublish('DRAFT', false)).toBe(true);
		});
		it('returns false for PUBLISHED', () => {
			expect(canPublish('PUBLISHED', false)).toBe(false);
		});
	});

	describe('canAddEntityMapping / canDeleteMapping', () => {
		it('returns true for DRAFT', () => {
			expect(canAddEntityMapping('DRAFT')).toBe(true);
			expect(canDeleteMapping('DRAFT')).toBe(true);
		});
		it('returns false for PUBLISHED', () => {
			expect(canAddEntityMapping('PUBLISHED')).toBe(false);
			expect(canDeleteMapping('PUBLISHED')).toBe(false);
		});
		it('returns false for VALIDATED', () => {
			expect(canAddEntityMapping('VALIDATED')).toBe(false);
			expect(canDeleteMapping('VALIDATED')).toBe(false);
		});
	});

	describe('isRevisionConflict', () => {
		it('returns true for 409', () => {
			expect(isRevisionConflict(409)).toBe(true);
		});
		it('returns false for 200', () => {
			expect(isRevisionConflict(200)).toBe(false);
		});
		it('returns false for 500', () => {
			expect(isRevisionConflict(500)).toBe(false);
		});
	});

	describe('getConflictMessage', () => {
		it('returns a non-empty message mentioning version conflict', () => {
			const msg = getConflictMessage();
			expect(msg).toContain('版本冲突');
			expect(msg).toContain('重新加载');
		});
	});
});
