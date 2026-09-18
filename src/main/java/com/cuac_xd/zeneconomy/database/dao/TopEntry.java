package com.cuac_xd.zeneconomy.database.dao;

import java.util.Map;
import java.util.UUID;

public record TopEntry(
        UUID uuid,
        String username,
        double balance,
        int rank
) {}
