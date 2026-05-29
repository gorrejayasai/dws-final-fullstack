package com.wallet.walletservice.mapper;

import com.wallet.walletservice.dto.request.CreateWalletRequest;
import com.wallet.walletservice.dto.response.WalletResponse;
import com.wallet.walletservice.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public Wallet toEntity(CreateWalletRequest req) {
        Wallet wallet = new Wallet();
        wallet.setUserId(req.getUserId());
        wallet.setCurrency(req.getCurrency());
        return wallet;
    }

    public WalletResponse toResponse(Wallet w) {
        return new WalletResponse(
                w.getId(),
                w.getUserId(),
                w.getCurrency(),
                w.getAvailableBalance(),
                w.getHeldBalance(),
                w.getStatus(),
                w.getCreatedAt()
        );
    }
}