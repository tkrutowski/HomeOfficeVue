<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { Fee, FeeInstallment } from "@/assets/types/Fee";
import { UtilsService } from "@/service/UtilsService";
import router from "@/router";
import moment from "moment";

import DeleteButton from "@/components/DeleteButton.vue";
import EditButton from "@/components/EditButton.vue";
import PayPaymentDialog from "@/components/PayPaymentDialog.vue";
import ConfirmationDialog from "@/components/ConfirmationDialog.vue";
import TheMenuFinance from "@/components/TheMenuFinance.vue";
import IconButton from "@/components/IconButton.vue";
import OfficeButton from "@/components/OfficeButton.vue";

import { useFeeStore } from "@/stores/fee";
import { usePaymentStore } from "@/stores/payments";
import { useToast } from "primevue/usetoast";

const feeStore = useFeeStore();
const paymentStore = usePaymentStore();
const toast = useToast();

const isBusy = ref<boolean>(false);

const fee = ref<Fee>();
const installments = ref<FeeInstallment[]>();

const props = defineProps({
  id: {
    type: Number,
    required: true,
  },
});

const countDeadLine = computed(() => {
  return fee.value?.installmentList[fee.value?.installmentList.length - 1]
    .paymentDeadline;
});
const plannedInterest = computed(() => {
  if (fee.value)
    return fee.value?.installmentList
      .map((installment) => installment.installmentAmountToPay)
      .reduce((accumulator, currentValue) => accumulator + currentValue, 0);
  return 0;
});
const currentInterest = computed(() => {
  if (fee.value)
    return fee.value.installmentList
      .filter((l) => l.paymentStatus.name === "PAID")
      .map(
        (installment) =>
          installment.installmentAmountPaid - installment.installmentAmountToPay
      )
      .reduce((accumulator, currentValue) => accumulator + currentValue, 0);
  return 0;
});
const realInterest = computed(() => {
  if (fee.value) {
    let length = fee.value.installmentList.filter(
      (installment) => installment.installmentAmountPaid !== 0
    ).length;
    if (length === fee.value.numberOfPayments)
      return fee.value?.installmentList
        .map((installment) => installment.installmentAmountPaid)
        .reduce((accumulator, currentValue) => accumulator + currentValue, 0);
  }
  return 0;
});
// ---------------------------------------------EDIT PAYMENT---------------------------------
const showPaymentModal = ref(false);
const installment = ref<FeeInstallment>();

function openPaymentModal(i: FeeInstallment) {
  installment.value = { ...i };
  showPaymentModal.value = true;
}

async function savePayment(date: string, amount: number) {
  if (installment.value) {
    isBusy.value = true;
    installment.value.paymentDate = moment(date).format("YYYY-MM-DD");
    installment.value.installmentAmountPaid = amount;
    installment.value.paymentStatus = { name: "PAID", viewName: "Spłacony" };
    showPaymentModal.value = false;
    const savedFee = await feeStore.updateFeeInstallmentDb(installment.value);
    //update views
    if (savedFee) {
      installments.value = savedFee.installmentList;
      paymentStore.updatePayment(savedFee, "FEE");
      toast.add({
        severity: "success",
        summary: "Potwierdzenie",
        detail: "Zaktualizowano płatność.",
        life: 3000,
      });
    } else {
      toast.add({
        severity: "error",
        summary: "Potwierdzenie",
        detail: "Błąd podczas płatność.",
        life: 3000,
      });
    }
  }
  isBusy.value = false;
}

//---------------------------------------------DELETE PAYMENT-----------------------------
const showDeleteConfirmationDialog = ref<boolean>(false);

const confirmDeletePayment = (i: FeeInstallment) => {
  installment.value = { ...i };
  showDeleteConfirmationDialog.value = true;
};

const deleteConfirmationMessage = computed(() => {
  if (installment.value)
    return `Czy chcesz usunąc płatność z dnia: <b>${installment.value?.paymentDate}</b> </br>&emsp;&emsp;&emsp; na kwotę <b>${installment.value?.installmentAmountPaid} zł</b>?`;
  return "No message";
});
const submitDelete = async () => {
  console.log("submitDelete()", installment.value);
  isBusy.value = true;
  if (installment.value) {
    installment.value.paymentStatus = {
      name: "TO_PAY",
      viewName: "Do zapłaty",
    };
    installment.value.paymentDate = "";
    installment.value.installmentAmountPaid = 0;
    showDeleteConfirmationDialog.value = false;
    const savedFee = await feeStore.updateFeeInstallmentDb(installment.value);
    //update views
    if (savedFee) {
      installments.value = savedFee.installmentList;
      paymentStore.updatePayment(savedFee, "FEE");
      toast.add({
        severity: "success",
        summary: "Potwierdzenie",
        detail: "Usunięto płatność.",
        life: 3000,
      });
    }
  }
  await refresh();
  isBusy.value = false;
};

const getAmount = computed(() => {
  return installment.value?.installmentAmountPaid
    ? installment.value.installmentAmountPaid
    : installment.value?.installmentAmountToPay;
});
const getDate = computed(() => {
  if (
    installment.value?.paymentDate !==
    ("-999999999-01-01" && "+1000000000-01-01")
  )
    return installment.value?.paymentDate;
  return moment().format("YYYY-MM-DD");
});
const isEdit = computed(() => {
  let isEdit = false;
  if (installment.value) isEdit = installment.value.installmentAmountPaid > 0;
  return isEdit;
});

//-----------------------------------------------------MOUNTED------------------------------------------
onMounted(async () => {
  console.log("onMounted GET");
  feeStore.getFees("ALL");
  refresh();
});
const refresh = async () => {
  fee.value = await feeStore.getFeeFromDb(+props.id);
  installments.value = fee.value ? fee.value?.installmentList : [];
};
</script>

<template>
  <TheMenuFinance />
  <PayPaymentDialog
    v-model:visible="showPaymentModal"
    :amount="getAmount"
    :date="getDate"
    :is-edit="isEdit"
    @save="savePayment"
    @cancel="showPaymentModal = false"
  />
  <ConfirmationDialog
    v-model:visible="showDeleteConfirmationDialog"
    :msg="deleteConfirmationMessage"
    label="Usuń"
    @save="submitDelete"
    @cancel="showDeleteConfirmationDialog = false"
  />
  <Panel id="fee-panel" class="mt-3 m-auto">
    <template #header>
      <IconButton
        v-tooltip.right="{
          value: 'Powrót do listy opłat',
          showDelay: 500,
          hideDelay: 300,
        }"
        icon="pi-fw pi-list"
        @click="() => router.push({ name: 'Fees' })"
      />
      <div class="w-full flex justify-content-center">
        <h2>
          {{ `Szczegóły opłaty: ${fee?.name}` }}
        </h2>
      </div>
    </template>
    <div class="formgrid grid">
      <!--   LEFT   -->
      <div class="field col">
        <p class="mb-1 mt-3"><small>Nazwa opłaty:</small> {{ fee?.name }}</p>
        <p class="mb-1"><small>Nazwa firmy:</small> {{ fee?.firm?.name }}</p>
        <p class="mb-1"><small>Nr umowy:</small> {{ fee?.feeNumber }}</p>
        <p class="mb-1"><small>Z dnia:</small> {{ fee?.date }}</p>
        <p class="mb-1">
          <small>Data pierwszej opłaty:</small> {{ fee?.firstPaymentDate }}
        </p>
        <p class="mb-1">
          <small>Termin całkowitej spłaty:</small> {{ countDeadLine }}
        </p>
        <p class="mb-5"><small>Nr konta:</small> {{ fee?.accountNumber }}</p>

        <p class="mb-1"><small>Kwota opłaty:</small> {{ fee?.amount }} zł</p>
        <p class="mb-1">
          <small>Częstotliwość opłat:</small>
          {{ fee?.feeFrequency?.viewName }}
        </p>

        <p class="mb-1">
          <small>Ilość opłat:</small> {{ fee?.numberOfPayments }}
        </p>
        <p class="mb-1">
          <small>Koszt planowany:</small>
          <span class="color-red ml-1">
            {{ UtilsService.formatCurrency(plannedInterest) }}</span
          >
        </p>

        <p class="mb-1">
          <small>Obecna różnica:</small>
          <span class="color-red ml-1"
            >{{ UtilsService.formatCurrency(currentInterest) }}
          </span>
        </p>

        <p class="mb-3 color-orange">
          <small>Koszt rzeczywisty:</small>
          <span class="color-red ml-1"
            >{{ UtilsService.formatCurrency(realInterest) }}
          </span>
        </p>
        <div class="flex flex-column">
          <label for="info">Opis:</label>
          <Textarea id="info" :v-model="fee?.otherInfo" rows="5" readonly />
        </div>
      </div>

      <!--      RIGHT TABLE -->
      <div class="field col">
        <DataTable :value="installments">
          <Column field="paymentDeadline" header="Termin płatności">
            <template #body="{ data, field }">
              <div style="text-align: center">
                {{ data[field] }}
              </div>
            </template>
          </Column>
          <Column field="installmentAmountToPay" header="Kwota">
            <template #body="{ data, field }">
              <div>
                {{ UtilsService.formatCurrency(data[field]) }}
              </div>
            </template>
          </Column>
          <Column field="paymentDate" header="Data płatności">
            <template #body="{ data, field }">
              <div style="text-align: center">
                {{ data[field].startsWith("+") ? "" : data[field] }}
              </div>
            </template>
          </Column>
          <Column field="installmentAmountPaid" header="Kwota">
            <template #body="{ data, field }">
              <div>
                {{
                  data[field] !== 0
                    ? UtilsService.formatCurrency(data[field])
                    : ""
                }}
              </div>
            </template>
          </Column>
          <!--                EDIT, DELETE-->
          <Column header="Akcja" :exportable="false" style="min-width: 8rem">
            <template #body="slotProps">
              <div class="flex flex-row gap-1 justify-content-end">
                <EditButton
                  v-tooltip.top="{
                    value: 'Edytuj wpłatę',
                    showDelay: 1000,
                    hideDelay: 300,
                  }"
                  @click="openPaymentModal(slotProps.data)"
                />
                <DeleteButton
                  v-tooltip.top="{
                    value: 'Usuń wpłatę',
                    showDelay: 1000,
                    hideDelay: 300,
                  }"
                  :disabled="slotProps.data.installmentAmountPaid === 0"
                  @click="confirmDeletePayment(slotProps.data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <template #footer>
      <div class="flex flex-row">
        <div class="flex col justify-content-center">
          <OfficeButton
            text="zamknij"
            btn-type="office"
            :btn-disabled="isBusy"
            :is-busy-icon="isBusy"
            @click="() => router.back()"
          />
        </div>
      </div>
    </template>
  </Panel>
</template>

<style scoped>
#fee-panel {
  max-width: 1000px;
}

.color-red {
  color: #dc3545;
}
</style>
