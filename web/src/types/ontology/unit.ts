export interface UnitCategory {
	id: string;
	categoryCode: string;
	categoryName: string;
	baseUnitSymbol: string;
	isBuiltin: '0' | '1';
}

export interface Unit {
	id: string;
	categoryId: string;
	unitCode: string;
	unitSymbol: string;
	unitName: string;
	isBaseUnit: '0' | '1';
	isBuiltin: '0' | '1';
	namespaceId?: string;
}
