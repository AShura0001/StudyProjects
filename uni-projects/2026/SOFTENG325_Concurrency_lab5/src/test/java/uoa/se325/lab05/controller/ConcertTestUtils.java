package uoa.se325.lab05.controller;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uoa.se325.lab05.model.Band;
import uoa.se325.lab05.model.Concert;
import uoa.se325.lab05.model.Genre;
import uoa.se325.lab05.model.IndividualArtist;
import uoa.se325.lab05.repository.ConcertRepository;
import uoa.se325.lab05.repository.PerformerRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Component
@Transactional
public class ConcertTestUtils {

    @Autowired
    public ConcertRepository concertRepository;

    @Autowired
    public PerformerRepository performerRepository;

    public Ids setUp() {
        // Delete any existing data
        concertRepository.deleteAll();
        performerRepository.deleteAll();

        Ids ids = new Ids();

        // Create some concerts
        var ptxWorldTour = new Concert("PTX: The World Tour",
                LocalDateTime.of(2020, 2, 15, 20, 0, 0));

        var hjShow = new Concert("Hugh Jackman: The Man. The Music. The Show.",
                LocalDateTime.of(2019, 9, 6, 20, 0, 0));

        concertRepository.save(ptxWorldTour);
        concertRepository.save(hjShow);
        ids.ptxWorldTourId = ptxWorldTour.getId();
        ids.hjShowId = hjShow.getId();

        // Create some artists
        var ptx = new Band("Pentatonix", Genre.Acappella,
                new IndividualArtist("Scott Hoying", Genre.Acappella, LocalDate.now()),
                new IndividualArtist("Mitch Grassi", Genre.Acappella, LocalDate.now()),
                new IndividualArtist("Kirstin Maldonado", Genre.Acappella, LocalDate.now()),
                new IndividualArtist("Kevin Olusola", Genre.Acappella, LocalDate.now()),
                new IndividualArtist("Matt Sallee", Genre.Acappella, LocalDate.now()));

        var hughJackman = new IndividualArtist("Hugh Jackman", Genre.Theatre, LocalDate.of(1968, 10, 12));
        var kealaSettle = new IndividualArtist("Keala Settle", Genre.Theatre, LocalDate.of(1975, 11, 5));

        performerRepository.save(ptx);
        performerRepository.save(hughJackman);
        performerRepository.save(kealaSettle);
        ids.ptxId = ptx.getId();
        ids.hughJackmanId = hughJackman.getId();
        ids.kealaSettleId = kealaSettle.getId();

        // Map concerts and artists
        ptxWorldTour.getPerformers().add(ptx);
        ptx.getConcerts().add(ptxWorldTour);
        concertRepository.save(ptxWorldTour);

        hjShow.getPerformers().add(hughJackman);
        hughJackman.getConcerts().add(hjShow);
        hjShow.getPerformers().add(kealaSettle);
        kealaSettle.getConcerts().add(hjShow);
        concertRepository.save(hjShow);

        return ids;
    }

    public void setSpecialTicketPrice(long concertId, int price) {
        // Apply a special to one of this concert's dates
        Concert concert = concertRepository.findById(concertId).orElse(null);
        assertNotNull(concert);
        concert.setSpecialTicketPrice(price);
        concertRepository.save(concert);
    }

    public static class Ids {
        public long ptxWorldTourId, hjShowId, ptxId, hughJackmanId, kealaSettleId;
    }
}
