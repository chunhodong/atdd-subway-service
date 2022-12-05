package nextstep.subway.favorite.application;

import nextstep.subway.favorite.domain.Favorite;
import nextstep.subway.favorite.domain.FavoriteRepository;
import nextstep.subway.favorite.dto.FavoriteRequest;
import nextstep.subway.favorite.dto.FavoriteResponse;
import nextstep.subway.favorite.exception.FavoriteException;
import nextstep.subway.member.application.MemberService;
import nextstep.subway.member.domain.Member;
import nextstep.subway.member.domain.MemberRepository;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.domain.StationRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import static nextstep.subway.favorite.exception.FavoriteExceptionCode.*;

@Service
public class FavoriteService {

    private final MemberService memberService;
    private final StationRepository stationRepository;
    private final FavoriteRepository favoriteRepository;

    public FavoriteService(MemberService memberService, StationRepository stationRepository, FavoriteRepository favoriteRepository) {
        this.memberService = memberService;
        this.stationRepository = stationRepository;
        this.favoriteRepository = favoriteRepository;
    }

    public FavoriteResponse createFavorite(Long memberId, FavoriteRequest favoriteRequest) {
        Member member = memberService.getMember(memberId);
        Station sourceStation = stationRepository.findById(favoriteRequest.getSourceId()).orElseThrow(() -> new FavoriteException(NONE_EXISTS_SOURCE_STATION));
        Station targetStation = stationRepository.findById(favoriteRequest.getTargetId()).orElseThrow(() -> new FavoriteException(NONE_EXISTS_TARGET_STATION));
        favoriteRepository.findByMemberAndSourceStationAndTargetStation(member, sourceStation, targetStation)
                .ifPresent(favorite -> {
                    throw new FavoriteException(ALREADY_REGISTER);
                });
        return FavoriteResponse.of(favoriteRepository.save(new Favorite(member,sourceStation,targetStation)));
    }

    public List<FavoriteResponse> findAllFavoriteByMember(Long memberId) {
        Member member = memberService.getMember(memberId);
        return favoriteRepository.findAllByMember(member)
                .stream()
                .map(favorite -> FavoriteResponse.of(favorite))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFavorite(Long favoriteId, Long memberId) {
        Member member = memberService.getMember(memberId);
        Favorite favorite = favoriteRepository.findById(favoriteId).orElseThrow(() -> new FavoriteException(NONE_EXISTS_FAVORITE));
        favorite.validateOwner(member);
        favoriteRepository.delete(favorite);
    }
}
