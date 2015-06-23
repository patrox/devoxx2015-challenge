package pl.allegro.promo.devoxx2015.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.promo.devoxx2015.domain.Offer;
import pl.allegro.promo.devoxx2015.domain.OfferRepository;
import pl.allegro.promo.devoxx2015.domain.PhotoScoreSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
public class OfferService {

    private final OfferRepository offerRepository;
    private final PhotoScoreSource photoScoreSource;

    @Autowired
    public OfferService(OfferRepository offerRepository, PhotoScoreSource photoScoreSource) {
        this.offerRepository = offerRepository;
        this.photoScoreSource = photoScoreSource;
    }

    public void processOffers(List<OfferPublishedEvent> events) {
        events.stream()
                .map(offerEvent -> {
                            double photoScore = 0.7;
                            try {
                                        photoScore = CompletableFuture
                                                .supplyAsync(() -> photoScoreSource.getScore(offerEvent.getPhotoUrl()))
                                                .get();
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                            return new Offer(offerEvent.getId(), offerEvent.getTitle(), offerEvent.getPhotoUrl(), photoScore);

                        }
                )
                .filter(Offer::hasPrettyPhoto)
                .forEach(offerRepository::save);

    }

    public List<Offer> getOffers() {
        return offerRepository.findAll()
                .stream()
                .sorted((o1, o2) -> -Double.compare(o1.getPhotoScore(), o2.getPhotoScore()))
                .collect(Collectors.toList());
    }
}
