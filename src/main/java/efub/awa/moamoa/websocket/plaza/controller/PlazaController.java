package efub.awa.moamoa.websocket.plaza.controller;

import efub.awa.moamoa.global.common.response.ApiResponse;
import efub.awa.moamoa.websocket.plaza.repository.PlazaParticipantRegistry;
import efub.awa.moamoa.websocket.plaza.dto.response.ParticipantResponse;
import efub.awa.moamoa.websocket.plaza.dto.response.PlazaSnapshotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plaza")
@RequiredArgsConstructor
public class PlazaController {

    private final PlazaParticipantRegistry registry;

    @GetMapping("/participants")
    public ApiResponse<PlazaSnapshotResponse> getPlazaParticipants() {
        //snapshotAt을 목록 조회보다 먼저 찍음
        //반대로 하면 그 사이에 일어난 변경이 스냅샷에도 없고 ts <= snapshotAt이라 버퍼에서도 버려져 유실
        //먼저 찍으면 일부 이벤트가 중복적용될 뿐, 입장/이동/퇴장 모두 여러 번 적용해도 결과가 같음
        long snapshotAt = System.currentTimeMillis();
        List<ParticipantResponse> list = registry.findAll().stream()
                .map(ParticipantResponse::from).toList();

        return ApiResponse.success("광장 참여자 조회 요청이 성공했습니다.",
                PlazaSnapshotResponse.of(snapshotAt, list));
    }
}
