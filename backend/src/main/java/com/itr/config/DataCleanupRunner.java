package com.itr.config;

import com.itr.entity.Client;
import com.itr.entity.ClientYearData;
import com.itr.entity.User;
import com.itr.repository.ClientRepository;
import com.itr.repository.ClientYearDataRepository;
import com.itr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataCleanupRunner implements CommandLineRunner {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientYearDataRepository yearDataRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // DB dump completed — no action needed
    }
}
