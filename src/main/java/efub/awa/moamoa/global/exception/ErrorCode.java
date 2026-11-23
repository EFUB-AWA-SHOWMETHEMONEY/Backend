package efub.awa.moamoa.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    //Default
    INTERNAL_SERVER_ERROR(500, "서버 내부 에러가 발생했습니다."),
    BAD_REQUEST(400, "잘못된 요청입니다."),

    //Auth - 401
    INVALID_CREDENTIALS(401, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_AUTHORIZATION_CODE(401, "유효하지 않은 인가코드입니다."),
    INVALID_REGISTER_TOKEN(401, "유효하지 않거나 만료된 가입 토큰입니다."),
    REFRESH_TOKEN_REQUIRED(401, "Refresh Token이 필요합니다."),
    INVALID_REFRESH_TOKEN(401, "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(401, "만료된 Refresh Token입니다."),
    ACCESS_TOKEN_REQUIRED(401, "Access Token이 필요합니다."),
    INVALID_ACCESS_TOKEN(401, "유효하지 않은 Access Token입니다."),
    EXPIRED_ACCESS_TOKEN(401, "만료된 Access Token입니다."),

    //Auth - 409
    DUPLICATE_LOGIN_ID(409, "이미 사용 중인 아이디입니다."),
    DUPLICATE_NICKNAME(409, "이미 사용 중인 닉네임입니다."),
    ALREADY_REGISTERED(409, "이미 가입된 소셜 계정입니다."),

    //Auth - 502
    OAUTH_SERVER_ERROR(502, "소셜 서버와 통신하지 못했습니다."),

    //WebSocket - 401
    INVALID_STOMP_AUTH_HEADER(401, "STOMP 인증 헤더가 필요합니다."),
    INVALID_AUTH_TOKEN(401, "유효하지 않은 인증 토큰입니다."),
    EXPIRED_AUTH_TOKEN(401, "만료된 인증 토큰입니다."),

    //Member - 404
    MEMBER_NOT_FOUND(404, "존재하지 않는 회원입니다."),

    //Member - 409
    WITHDRAWAL_NOT_ALLOWED(409, "진행 중인 경매 또는 거래가 있어 탈퇴할 수 없습니다."),

    //Attendance
    ALREADY_RECEIVED_REWARD(409, "이미 오늘의 보상을 받았습니다."),

    //Websocket
    INVALID_STOMP_DESTINATION(400, "잘못된 STOMP 도착지입니다"),

    //Plaza - 400
    NOT_IN_ROOM(400, "광장에 존재하지 않는 회원입니다."),

    //Auction - 400
    NOT_IN_AUCTION_ROOM(400, "경매에 존재하지 않는 회원입니다"),
    NOT_AUCTION_PARTICIPANT(403, "경매에 참여한 사용자만 커밋할 수 있습니다."),
    AUCTION_NOT_FOUND(404, "존재하지 않는 경매입니다"),
    ALREADY_STARTED(400, "이미 시작된 경매에는 새로 입장할 수 없습니다."),
    ALREADY_ENDED(400, "이미 종료된 경매입니다."),
    NOT_PRIVATE_AUCTION(400, "PRIVATE 경매가 아닙니다."),
    INVALID_AUCTION_STATE(400, "커밋 제출이 불가능한 경매 상태입니다."),
    INVALID_REVEAL_STATE (400, "지금은 공개 가능한 시간이 아닙니다."),
    NO_ACTIVE_COMMIT(400, "제출한 입찰 커밋이 없습니다."),
    ALREADY_REVEALED(409, "이미 공개한 입찰입니다."),
    COMMIT_HASH_MISMATCH(400, "제출한 금액이 커밋 해시와 일치하지 않습니다."),

    INVALID_READY_START_TIME(400, "경매 입장 시작 시각은 현재 시각 이후여야 합니다."),
    ITEM_EQUIPPED(400, "장착 중인 아이템은 경매에 올릴 수 없습니다."),
    INSUFFICIENT_ITEM_QUANTITY(400, "경매에 올릴 수 있는 수량이 없습니다."),
    AUCTION_CONDITION_NOT_MET(409, "아직 전환 조건을 만족하지 않았습니다."),
    ENTRY_NOT_STARTED(400, "아직 입장 시간이 아닙니다."),
    SELLER_CANNOT_JOIN(400, "본인이 등록한 경매에는 입장할 수 없습니다."),
    ROOM_FULL(409, "정원이 가득 찼습니다."),
    REENTRY_NOT_ALLOWED(409, "PUBLIC 경매는 시작 후 퇴장하면 다시 입장할 수 없습니다."),

    //Item
    ITEM_NOT_FOUND(404, "존재하지 않는 아이템입니다."),
    ITEM_NOT_FOR_SALE(400, "상점에서 판매하지 않는 아이템입니다."),

    //Inventory
    INVENTORY_NOT_FOUND(404, "인벤토리에 존재하지 않는 아이템입니다."),

    //Transfer
    INVALID_TRANSFER_TARGET(400, "자기 자신과는 거래할 수 없습니다."),

    //Balance
    RESOURCE_BUSY(409, "요청이 많아 처리에 실패했습니다. 다시 시도해주세요"),
    INSUFFICIENT_BALANCE(400, "잔액이 부족합니다"),
    INVALID_AMOUNT(400, "금액은 0보다 커야합니다.");

    private final int status;
    private final String message;
}
