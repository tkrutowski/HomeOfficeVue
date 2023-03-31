package net.focik.homeoffice.goahead.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.goahead.api.dto.BasicDto;
import net.focik.homeoffice.goahead.api.dto.CustomerDto;
import net.focik.homeoffice.goahead.api.dto.CustomerTypeDto;
import net.focik.homeoffice.goahead.api.mapper.ApiCustomerMapper;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.customer.CustomerType;
import net.focik.homeoffice.goahead.domain.customer.port.primary.AddCustomerUseCase;
import net.focik.homeoffice.goahead.domain.customer.port.primary.DeleteCustomerUseCase;
import net.focik.homeoffice.goahead.domain.customer.port.primary.GetCustomerUseCase;
import net.focik.homeoffice.goahead.domain.customer.port.primary.UpdateCustomerUseCase;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import net.focik.homeoffice.utils.exceptions.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/goahead/customer")
//@CrossOrigin
public class CustomerController extends ExceptionHandling {

    private final ApiCustomerMapper mapper;
    private final AddCustomerUseCase addCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final DeleteCustomerUseCase deleteCustomerUseCase;

    @GetMapping("/test")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    String test() {
        return "test";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    ResponseEntity<CustomerDto> getById(@PathVariable int id,
                                        @RequestParam Boolean isAddress) {

        log.info("Try find customer by id: " + id);

        Customer customer = getCustomerUseCase.findById(id, isAddress);

        log.info(customer != null ? "Found customer for id = " + id : "Not found customer for id = " + id);

        if (customer == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(mapper.toDto(customer), OK);
    }

    @GetMapping()
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    ResponseEntity<List<CustomerDto>> getAllCustomers(@RequestParam(required = false) CustomerStatus status,
                                                      @RequestParam(required = false) CustomerType type,
                                                      @RequestParam(required = false) Boolean address) {
        log.info("Try find all employee by EmploymentStatus = " + status);

        List<Customer> customerList = getCustomerUseCase.findByAll(status, address, type);

        log.info("Found " + customerList.size() + " employees.");

        return new ResponseEntity<>(customerList.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList()), OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<Integer> addCustomer(@RequestBody CustomerDto customerDto) {
        log.info("Try add new customer.");

        Customer customer = mapper.toDomain(customerDto);
        Integer result = addCustomerUseCase.addCustomer(customer);

        log.info(result > 0 ? "Customer added with id = " + result : "No customer added!");

        if (result <= 0)
            return new ResponseEntity<>(0, HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<CustomerDto> updateEmployee(@RequestBody CustomerDto customerDto) {
        log.info("Try update customer with id: {}", customerDto.getId());

        Customer customer = updateCustomerUseCase.updateCustomer(mapper.toDomain(customerDto));
        return new ResponseEntity<>(mapper.toDto(customer), OK);
    }

    @DeleteMapping("/{idCustomer}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_DELETE_ALL')")
    public ResponseEntity<HttpResponse> deleteCustomer(@PathVariable int idCustomer) {
        log.info("Try delete customer with id: " + idCustomer);

        deleteCustomerUseCase.deleteCustomer(idCustomer);

        log.info("Deleted customer with id = " + idCustomer);

        return response(HttpStatus.NO_CONTENT, "Klient usunięty.");
    }

    @GetMapping("/customertype")
    ResponseEntity<List<CustomerTypeDto>> getCustomerTypes() {

        CustomerType[] collect = (CustomerType.values());

        List<CustomerTypeDto> customerTypeDtos = Arrays.stream(collect)
                .map(type -> new CustomerTypeDto(type.name(), type.getViewValue()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(customerTypeDtos, OK);
    }

    @PutMapping("/customerstatus/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<HttpResponse> updateEmploymentStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Try update customer status.");

        updateCustomerUseCase.updateCustomerStatus(id, CustomerStatus.valueOf(basicDto.getValue()));
        return response(HttpStatus.OK, "Zaaktualizowano status pracownika.");
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        HttpResponse body = new HttpResponse(status.value(), status, status.getReasonPhrase(), message);
        return new ResponseEntity<>(body, status);
    }
}