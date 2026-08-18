package com.silo.contribution;

import java.time.LocalDate;
import java.util.List;

public interface AutoDebitMandateLookup {

    List<DueAutoDebitMandate> findDueMandates(LocalDate onOrBefore);
}
