export interface OutboxQuery {
	eventType?: string;
	status?: string;
	ontologyId?: string;
	eventId?: string;
	aggregateType?: string;
	aggregateId?: string;
	current?: number;
	size?: number;
}

export interface OutboxVO {
	id: string;
	eventId: string;
	eventType: string;
	eventVersion: number;
	ontologyId?: string;
	aggregateType?: string;
	aggregateId?: string;
	operation?: string;
	occurredAt: string;
	payload?: string;
	metadata?: string;
	traceId?: string;
	actorId?: string;
	status: string;
	availableAt: string;
	leaseUntil?: string;
	lockedBy?: string;
	deliveryAttempt: number;
	streamRecordId?: string;
	publishedAt?: string;
	lastErrorCode?: string;
	lastErrorMessage?: string;
	createTime: string;
	payloadSummary?: string;
}

export interface DeadLetterQuery {
	consumerGroup?: string;
	eventType?: string;
	status?: string;
	eventId?: string;
	current?: number;
	size?: number;
}

export interface DeadLetterVO {
	id: string;
	consumerGroup: string;
	eventId: string;
	replayNo: number;
	eventType: string;
	streamRecordId: string;
	payload?: string;
	failureCategory: string;
	errorCode?: string;
	errorMessage?: string;
	deliveryCount: number;
	status: string;
	replayedAsNo?: number;
	firstFailedAt: string;
	lastFailedAt: string;
	resolvedAt?: string;
	createTime: string;
	payloadSummary?: string;
}

export interface EventOverviewVO {
	outboxPending: number;
	outboxProcessing: number;
	outboxPublished: number;
	outboxFailed: number;
	streamLength: number;
	deadLetterOpen: number;
	redisAvailable: boolean;
}

export interface ConsumerGroupVO {
	consumerGroup: string;
	pendingCount: number;
	lag: number;
	online: boolean;
}

export interface ReplayRequest {
	eventId: string;
	targetConsumerGroup: string;
	reason: string;
	approvalRequestNo?: string;
	sourceReplayNo: number;
}

export interface ResolveRequest {
	action: string;
	remark?: string;
}
