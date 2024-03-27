package rs.edu.raf.banka1.services;

import rs.edu.raf.banka1.model.ListingForex;
import rs.edu.raf.banka1.model.ListingFuture;
import rs.edu.raf.banka1.model.ListingHistory;

import java.util.List;
import java.util.Optional;

public interface FuturesService {
    List<ListingFuture> fetchNFutures(int n);

    List<ListingHistory> fetchNFutureHistories(List<ListingFuture> listingFutures, int n);

    int addAllFutures(List<ListingFuture> futures);

    int addFuture(ListingFuture future);

    Optional<ListingFuture> findById(Long id);
}
