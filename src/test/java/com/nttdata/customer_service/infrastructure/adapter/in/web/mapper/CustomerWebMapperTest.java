package com.nttdata.customer_service.infrastructure.adapter.in.web.mapper;

import com.nttdata.bootcamp.customer.dto.CustomerUpdateRequest;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerCommand;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomerWebMapperTest {

    private final CustomerWebMapper mapper = new CustomerWebMapper();

    @Test
    void toUpdateCommand_withoutProfile_mapsProfileAsNull() {
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setProfile(null);
        UpdateCustomerCommand command = mapper.toUpdateCommand(request);

        assertThat(command.profile()).isNull();
    }

}
