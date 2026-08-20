package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqGroupMemberMuteRequestDto {

    private List<SetMemberMuteState> members;

    public static QqGroupMemberMuteRequestDto of(List<SetMemberMuteState> members) {
        QqGroupMemberMuteRequestDto request = new QqGroupMemberMuteRequestDto();
        request.setMembers(members);
        return request;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SetMemberMuteState {
        public static final String OP_ADD = "add";
        public static final String OP_UPDATE = "update";
        public static final String OP_DELETE = "del";

        private String op;

        @JsonProperty("member_openid")
        private String memberOpenid;

        @JsonProperty("mute_expire_at")
        private String muteExpireAt;

        public static SetMemberMuteState add(String memberOpenid, String muteExpireAt) {
            return of(OP_ADD, memberOpenid, muteExpireAt);
        }

        public static SetMemberMuteState update(String memberOpenid, String muteExpireAt) {
            return of(OP_UPDATE, memberOpenid, muteExpireAt);
        }

        public static SetMemberMuteState delete(String memberOpenid) {
            return of(OP_DELETE, memberOpenid, "");
        }

        public static SetMemberMuteState of(String op, String memberOpenid, String muteExpireAt) {
            SetMemberMuteState item = new SetMemberMuteState();
            item.setOp(op);
            item.setMemberOpenid(memberOpenid);
            item.setMuteExpireAt(muteExpireAt);
            return item;
        }
    }
}