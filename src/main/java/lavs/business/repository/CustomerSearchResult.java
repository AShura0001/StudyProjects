package lavs.business.repository;

import lavs.business.model.Customer;

import java.util.List;

public record CustomerSearchResult(
        List<Customer> customers,
        long total,
        long page
) {
}