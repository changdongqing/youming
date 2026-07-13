import request from '/@/utils/request';
import type {
	ConsumerGroupVO,
	DeadLetterQuery,
	DeadLetterVO,
	EventOverviewVO,
	OutboxQuery,
	OutboxVO,
	ReplayRequest,
	ResolveRequest,
} from '/@/types/ontology/event';

const BASE = '/admin/ontology/events';

/** 事件概览 */
export const fetchEventOverview = () => {
	return request<EventOverviewVO>({ url: `${BASE}/overview`, method: 'get' });
};

/** Outbox 分页 */
export const fetchOutboxPage = (query?: OutboxQuery) => {
	return request({ url: `${BASE}/outbox/page`, method: 'get', params: query });
};

/** 重试失败投递 */
export const retryOutboxObj = (id: string | number) => {
	return request({ url: `${BASE}/outbox/${id}/retry`, method: 'post' });
};

/** 消费者组状态 */
export const fetchConsumerGroups = () => {
	return request<ConsumerGroupVO[]>({ url: `${BASE}/consumers`, method: 'get' });
};

/** 死信分页 */
export const fetchDeadLetterPage = (query?: DeadLetterQuery) => {
	return request({ url: `${BASE}/dead-letters/page`, method: 'get', params: query });
};

/** 死信回放 */
export const replayDeadLetterObj = (id: string | number, remark?: string) => {
	return request({ url: `${BASE}/dead-letters/${id}/replay`, method: 'post', data: { remark } });
};

/** 受控回放 */
export const createReplay = (data: ReplayRequest) => {
	return request({ url: `${BASE}/replays`, method: 'post', data });
};

/** 死信处理 */
export const resolveDeadLetterObj = (id: string | number, data: ResolveRequest) => {
	return request({ url: `${BASE}/dead-letters/${id}/resolve`, method: 'post', data });
};
