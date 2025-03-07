package com.tamaspinter.instantpaymentapi.repository;

import com.tamaspinter.instantpaymentapi.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
}
