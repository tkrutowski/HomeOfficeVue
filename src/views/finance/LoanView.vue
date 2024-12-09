<script setup lang="ts">
import { useLoansStore } from "@/stores/loans";
import { useBanksStore } from "@/stores/banks";
import { useUsersStore } from "@/stores/users";
import { useRoute } from "vue-router";
import { computed, onMounted, ref, watch } from "vue";
import { Loan } from "@/types/Loan";
import moment from "moment";
import OfficeButton from "@/components/OfficeButton.vue";
import { useToast } from "primevue/usetoast";
import router from "@/router";
import { Bank } from "@/types/Bank";
import { User } from "@/types/User";
import TheMenuFinance from "@/components/finance/TheMenuFinance.vue";
import OfficeIconButton from "@/components/OfficeIconButton.vue";

const userStore = useUsersStore();
const loanStore = useLoansStore();
const bankStore = useBanksStore();
const route = useRoute();

const toast = useToast();
const selectedUser = ref<User | undefined>();
const selectedBank = ref<Bank | undefined>();

const loan = ref<Loan>({
  id: 0,
  bank: undefined,
  idUser: 0,
  name: "",
  amount: 0,
  date: new Intl.DateTimeFormat("pl-PL", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  })
    .format(new Date())
    .split(".")
    .reverse()
    .join("-"),
  loanNumber: "",
  accountNumber: "",
  firstPaymentDate: new Intl.DateTimeFormat("pl-PL", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  })
    .format(new Date())
    .split(".")
    .reverse()
    .join("-"),
  numberOfInstallments: 1,
  installmentAmount: 0,
  loanStatus: { name: "TO_PAY", viewName: "Do spłaty" },
  loanCost: 0,
  otherInfo: "",
  installmentList: [],
});

const btnShowError = ref<boolean>(false);
const btnShowBusy = ref<boolean>(false);
const btnShowOk = ref<boolean>(false);
const btnSaveDisabled = ref<boolean>(false);

const isSaveBtnDisabled = computed(() => {
  return (
    loanStore.loadingPaymentType ||
    loanStore.loadingLoans ||
    userStore.loadingUsers ||
    bankStore.loadingBanks ||
    btnSaveDisabled.value
  );
});
const loanDateTemp = ref<string>("");
watch(loanDateTemp, (newDate: string) => {
  loan.value.date = moment(new Date(newDate)).format("YYYY-MM-DD");
});
const firstPaymentDateTemp = ref<string>("");
watch(firstPaymentDateTemp, (newDate: string) => {
  loan.value.firstPaymentDate = moment(new Date(newDate)).format("YYYY-MM-DD");
});
//
//------------------------------------------------------SAVE-----------------------------------------
//
function saveLoan() {
  submitted.value = true;
  if (isEdit.value) {
    editLoan();
  } else {
    newLoan();
  }
}
//
//---------------------------------------------------------NEW LOAN----------------------------------------------
//
async function newLoan() {
  console.log("newLoan()");
  if (isNotValid()) {
    showError("Uzupełnij brakujące elementy");
    btnShowError.value = true;
    setTimeout(() => (btnShowError.value = false), 5000);
  } else {
    btnSaveDisabled.value = true;

    const result = await loanStore.addLoanDb(loan.value);

    if (result) {
      toast.add({
        severity: "success",
        summary: "Potwierdzenie",
        detail: "Zapisano kredyt: " + loan.value?.name,
        life: 3000,
      });
      btnShowOk.value = true;
      setTimeout(() => {
        router.push({ name: "Loans" });
      }, 3000);
    } else {
      toast.add({
        severity: "error",
        summary: "Błąd",
        detail: "Błąd podczas zapisywania kredytu",
        life: 3000,
      });
    }
    btnShowError.value = true;
    btnSaveDisabled.value = false;

    setTimeout(() => {
      btnShowError.value = false;
      btnShowOk.value = false;
    }, 5000);
  }
}

//
//-----------------------------------------------------EDIT LOAN------------------------------------------------
//
const isEdit = ref<boolean>(false);
async function editLoan() {
  if (isNotValid()) {
    showError("Uzupełnij brakujące elementy");
    btnShowError.value = true;
    setTimeout(() => (btnShowError.value = false), 5000);
  } else {
    btnSaveDisabled.value = true;
    console.log("editLoan()");
    const result = await loanStore.updateLoanDb(loan.value);

    if (result) {
      toast.add({
        severity: "success",
        summary: "Potwierdzenie",
        detail: "Zaaktualizowano kredyt: " + loan.value?.name,
        life: 3000,
      });
      btnShowOk.value = true;
      setTimeout(() => {
        router.push({ name: "Loans" });
      }, 3000);
    } else btnShowError.value = true;

    // btnSaveDisabled.value = false;
    setTimeout(() => {
      btnShowError.value = false;
      btnShowOk.value = false;
      btnShowError.value = false;
    }, 5000);
  }
}
//---------------------------------------MOUNTED------------------------------------------------
onMounted(() => {
  console.log("onMounted GET");
  btnSaveDisabled.value = true;
  if (userStore.users.length === 0) userStore.getUsersFromDb();
  if (bankStore.banks.length === 0) bankStore.getBanksFromDb();
  btnSaveDisabled.value = false;
  console.log("loanStore.loans.length", loanStore.loans.length);
  loanStore.getLoans("ALL");
});

onMounted(async () => {
  // console.log("onMounted EDIT", route.params);
  btnSaveDisabled.value = true;
  isEdit.value = route.params.isEdit === "true";
  if (isEdit.value === false) {
    console.log("onMounted NEW LOAN");
    // invoiceDateTemp.value = loan.value.invoiceDate;
    // sellDateTemp.value = loan.value.sellDate;
  } else {
    console.log("onMounted EDIT LOAN");
    const loanId = Number(route.params.loanId as string);
    loanStore
      .getLoanFromDb(loanId)
      .then((data) => {
        if (data) {
          loan.value = data;
          selectedBank.value = loan.value.bank;
          selectedUser.value = userStore.getUser(loan.value.idUser);
          loanDateTemp.value = loan.value.date;
          firstPaymentDateTemp.value = loan.value.firstPaymentDate;
        }
      })
      .catch((error) => {
        console.error("Błąd podczas pobierania kredytu:", error);
      });
  }
  btnSaveDisabled.value = false;
});

//
//------------------------------------------ERROR
//
const submitted = ref(false);

const showError = (msg: string) => {
  toast.add({
    severity: "error",
    summary: "Error Message",
    detail: msg,
    life: 3000,
  });
};
const isNotValid = () => {
  return (
    showErrorName() ||
    showErrorUser() ||
    showErrorBank() ||
    showErrorNumber() ||
    showErrorAmount() ||
    showErrorAccountNumber() ||
    showErrorInstallmentAmount() ||
    showErrorFirstDate() ||
    showErrorDate()
  );
};
const showErrorName = () => {
  return submitted.value && loan.value.name.length === 0;
};
const showErrorUser = () => {
  return submitted.value && loan.value.idUser === 0;
};
const showErrorBank = () => {
  return submitted.value && !loan.value.bank;
};
const showErrorNumber = () => {
  return submitted.value && loan.value.loanNumber.length === 0;
};
const showErrorAmount = () => {
  return submitted.value && loan.value.amount <= 0;
};
const showErrorInstallmentAmount = () => {
  return submitted.value && loan.value.installmentAmount <= 0;
};
const showErrorAccountNumber = () => {
  //todo zrobić regex
  return submitted.value && loan.value.accountNumber.length === 0;
};
const showErrorDate = () => {
  return submitted.value && loanDateTemp.value.length === 0;
};
const showErrorFirstDate = () => {
  return submitted.value && firstPaymentDateTemp.value.length === 0;
};
</script>

<template>
  <Toast />
  <TheMenuFinance />

  <div class="m-4 max-w-4xl mx-auto">
    <form @submit.stop.prevent="saveLoan" class="w-full">
      <Panel class="w-full">
        <template #header>
          <OfficeIconButton
            v-tooltip.right="{
              value: 'Powrót do listy kredytów',
              showDelay: 500,
              hideDelay: 300,
            }"
            icon="pi pi-fw pi-list"
            @click="() => router.push({ name: 'Loans' })"
          />
          <div class="w-full flex justify-center">
            <h2>
              {{ isEdit ? `Edycja kredytu: ${loan?.name}` : "Nowy kredyt" }}
            </h2>
          </div>
        </template>
          <div class="flex flex-col ">
            <!-- ROW-1   NAME -->
            <div class="flex flex-col">
              <label for="name">Nazwa</label>
              <InputText
                id="name"
                v-model="loan.name"
                maxlength="50"
                :class="{ 'p-invalid': showErrorName() }"
              />
              <small class="p-error">{{
                showErrorName() ? "Pole jest wymagane." : "&nbsp;"
              }}</small>
            </div>

            <!-- ROW-2   USER -->
            <div class="flex flex-row gap-4 ">
              <div class="flex flex-col w-full">
                <label for="input-customer"
                  >Wybierz użytkownika:</label
                >
                <Select
                  id="input-customer"
                  v-model="selectedUser"
                  :class="{ 'p-invalid': showErrorUser() }"
                  :options="userStore.users"
                  :option-label="(user) => user.firstName + ' ' + user.lastName"
                  @change="(loan.idUser = selectedUser ? selectedUser.id : 0)"
                />
                <small class="p-error">{{
                  showErrorUser() ? "Pole jest wymagane." : "&nbsp;"
                }}</small>
              </div>
              <div v-if="userStore.loadingUsers" class="content-center">
                <ProgressSpinner
                  class="ml-2 mt-1"
                  style="width: 30px; height: 30px"
                  stroke-width="5"
                />
              </div>
            </div>

            <!-- ROW-3   BANK -->
            <div class="flex flex-row gap-4">
              <div class="flex flex-col w-full">
                <label for="input-customer"
                  >Wybierz bank:</label
                >
                <Select
                  id="input-customer"
                  v-model="selectedBank"
                  :class="{ 'p-invalid': showErrorBank() }"
                  :options="bankStore.banks"
                  option-label="name"
                  :onchange="
                    (loan.bank = selectedBank ? selectedBank : undefined)
                  "
                />
                <small class="p-error">{{
                  showErrorBank() ? "Pole jest wymagane." : "&nbsp;"
                }}</small>
              </div>
              <div v-if="bankStore.loadingBanks" class="content-center">
                <ProgressSpinner
                  class="ml-2 mt-1"
                  style="width: 30px; height: 30px"
                  stroke-width="5"
                />
              </div>
            </div>

            <!-- ROW-4  NUMBER / DATE  -->
            <div class="flex flex-col md:flex-row gap-4">
              <div class="flex flex-col w-full">
                <label for="number">Numer kredytu</label>
                <InputText
                  id="number"
                  v-model="loan.loanNumber"
                  :class="{ 'p-invalid': showErrorNumber() }"
                  maxlength="50"
                />
                <small class="p-error">{{
                  showErrorNumber() ? "Pole jest wymagane." : "&nbsp;"
                }}</small>
              </div>
              <div class="flex flex-col w-full">
                <label for="date">Z dnia:</label>
                <DatePicker
                  id="date"
                  v-model="loanDateTemp"
                  show-icon
                  date-format="yy-mm-dd"
                  :class="{ 'p-invalid': showErrorDate() }"
                />
                <small class="p-error">{{
                  showErrorDate() ? "Pole jest wymagane." : "&nbsp;"
                }}</small>
              </div>
            </div>

            <!-- ROW-5  AMOUNT / COST  -->
            <div class="flex flex-col md:flex-row gap-4">
              <div class="flex flex-col w-full">
                <label for="amount">Kwota kredytu</label>
                <InputNumber
                  id="amount"
                  v-model="loan.amount"
                  :class="{ 'p-invalid': showErrorAmount() }"
                  :min-fraction-digits="2"
                  :max-fraction-digits="2"
                  mode="currency" currency="PLN" locale="pl-PL"
                />
                <small class="p-error">{{
                  showErrorAmount() ? "Pole jest wymagane." : "&nbsp;"
                }}</small>
              </div>
              <div class="flex flex-col w-full">
                <label for="cost">Koszt kredytu:</label>
                <InputNumber
                  id="cost"
                  v-model="loan.loanCost"
                  :min-fraction-digits="2"
                  :max-fraction-digits="2"
                  mode="currency" currency="PLN" locale="pl-PL"
                />
              </div>
            </div>

            <!-- ROW-5  INSTALLMENT NR / INSTALLMENT AMOUNT  -->
            <div class="flex flex-col md:flex-row gap-4">
              <div class="flex flex-col w-full">
                <label for="amount">Ilość rat:</label>
                <InputNumber
                  id="amount"
                  v-model="loan.numberOfInstallments"
                  mode="decimal"
                  show-buttons
                  :min="1"
                  :max="84"
                  :disabled="isEdit"
                />
              </div>
              <div class="flex flex-col w-full">
                <label for="installmentAmount">Kwota raty:</label>
                <InputNumber
                  id="installmentAmount"
                  v-model="loan.installmentAmount"
                  :class="{ 'p-invalid': showErrorInstallmentAmount() }"
                  :min-fraction-digits="2"
                  :max-fraction-digits="2"
                  :disabled="isEdit"
                  mode="currency" currency="PLN" locale="pl-PL"
                />
                <small class="p-error">{{
                  showErrorInstallmentAmount()
                    ? "Pole jest wymagane."
                    : "&nbsp;"
                }}</small>
              </div>
            </div>

            <!-- ROW-6  ACCOUNT NR / FIRST PAYMENT DATE  -->
            <div class="flex flex-col md:flex-row gap-4">
              <div class="flex flex-col w-full">
                <label for="accountNo">Nr konta:</label>
                <InputMask
                  id="accountNo"
                  v-model="loan.accountNumber"
                  :class="{ 'p-invalid': showErrorAccountNumber() }"
                  mask="99 9999 9999 9999 9999 9999 9999"
                />
                <small class="p-error">{{
                  showErrorAccountNumber() ? "Pole jest wymagane." : "&nbsp;"
                }}</small>
              </div>
              <div class="flex flex-col w-full">
                <label for="first"
                  >Data pierwszej raty:</label
                >
                <DatePicker
                  id="first"
                  v-model="firstPaymentDateTemp"
                  :class="{ 'p-invalid': showErrorFirstDate() }"
                  show-icon
                  date-format="yy-mm-dd"
                  :disabled="isEdit"
                />
                <small class="p-error">{{
                  showErrorFirstDate() ? "Pole jest wymagane." : "&nbsp;"
                }}</small>
              </div>
            </div>

            <!-- ROW-7  OTHER INFO  -->
            <div class="flex flex-col w-full">
                <label for="input"
                  >Dodatkowe informacje:</label
                >
                <Textarea v-model="loan.otherInfo" rows="5" cols="30" />
              </div>
          </div>

        <!-- ROW-6  BTN SAVE -->
          <div class="flex mt-5 justify-end">
            <OfficeButton
              text="zapisz"
              btn-type="office-save"
              type="submit"
              :is-busy-icon="btnShowBusy"
              :is-error-icon="btnShowError"
              :is-ok-icon="btnShowOk"
              :btn-disabled="isSaveBtnDisabled"
            />
        </div>
      </Panel>
    </form>
  </div>
</template>

<style scoped></style>
