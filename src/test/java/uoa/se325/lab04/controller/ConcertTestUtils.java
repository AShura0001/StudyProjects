package uoa.se325.lab04.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uoa.se325.lab04.model.concert.Band;
import uoa.se325.lab04.model.concert.Concert;
import uoa.se325.lab04.model.concert.Genre;
import uoa.se325.lab04.model.concert.IndividualArtist;
import uoa.se325.lab04.repository.ConcertRepository;
import uoa.se325.lab04.repository.PerformerRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class ConcertTestUtils {

    @Autowired
    public ConcertRepository concertRepository;

    @Autowired
    public PerformerRepository performerRepository;

    // To set during the setUp() method so we can refer to the db-created IDs later on.
    public long ptxWorldTourId, hjShowId, ptxId, hughJackmanId, kealaSettleId;

    public void setUp() {
        // Delete any existing data
        concertRepository.deleteAll();
        performerRepository.deleteAll();

        // Create some concerts
        var ptxWorldTour = new Concert("PTX: The World Tour",
                LocalDateTime.of(2020, 2, 15, 20, 0, 0));

        var hjShow = new Concert("Hugh Jackman: The Man. The Music. The Show.",
                LocalDateTime.of(2019, 9, 6, 20, 0, 0),
                LocalDateTime.of(2019, 9, 7, 20, 0, 0));

        concertRepository.save(ptxWorldTour);
        concertRepository.save(hjShow);
        this.ptxWorldTourId = ptxWorldTour.getId();
        this.hjShowId = hjShow.getId();

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
        this.ptxId = ptx.getId();
        this.hughJackmanId = hughJackman.getId();
        this.kealaSettleId = kealaSettle.getId();

        // Map concerts and artists
        ptxWorldTour.getPerformers().add(ptx);
        ptx.getConcerts().add(ptxWorldTour);
        concertRepository.save(ptxWorldTour);

        hjShow.getPerformers().add(hughJackman);
        hughJackman.getConcerts().add(hjShow);
        hjShow.getPerformers().add(kealaSettle);
        kealaSettle.getConcerts().add(hjShow);
        concertRepository.save(hjShow);
    }
}
