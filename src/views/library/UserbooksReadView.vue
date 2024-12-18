<script setup lang="ts">
import TheMenuLibrary from '../../components/library/TheMenuLibrary.vue'
import {useUserbooksStore} from '../../stores/userbooks'
import UserBookSmall from '../../components/library/UserBookSmall.vue'
import {ref} from 'vue'
import type {UserBook} from '../../types/Book'
import AddEditUserBookDialog from '../../components/library/AddEditUserBookDialog.vue'
import {useToast} from 'primevue/usetoast'
import type {AxiosError} from "axios";

const toast = useToast()
const userbookStore = useUserbooksStore()
const selectedYear = ref<number>(new Date().getFullYear())
const displayedYear = ref<number>(new Date().getFullYear())
const userbooks = ref<UserBook[]>([])
if (userbookStore.userbooks.length === 0) userbookStore.getUserbooksFromDb()

async function getUserbooks() {
  userbooks.value = userbookStore.getUserbooksByDate(selectedYear.value)
  displayedYear.value = selectedYear.value
}

function getTotalAudiobook(edition: string): number {
  return userbooks.value.filter((ub: UserBook) => ub.editionType.name === edition).length
}

//
//-------------------------------------------------USERBOOK-------------------------------------------------
//
const showUserbookDialog = ref<boolean>(false)
const tempUserbook = ref<UserBook>()
const editUserbook = (newUserbook: UserBook) => {
  tempUserbook.value = newUserbook
  showUserbookDialog.value = true
}
const submitEditUserbook = async (newUserbook: UserBook) => {
  showUserbookDialog.value = false
  if (newUserbook) {
    await userbookStore.updateUserbookDb(newUserbook)
        .then(() => {
          toast.add({
            severity: 'success',
            summary: 'Potwierdzenie',
            detail: 'Zaaktualizowano książkę na półce: ' + newUserbook.book?.title,
            life: 3000,
          })
        }).catch((reason: AxiosError) => {
          toast.add({
            severity: 'error',
            summary: reason?.message,
            detail: 'Błąd podczas aktualizacji książki na półkę.',
            life: 3000,
          })
        })
  }
}
</script>

<template>
  <TheMenuLibrary/>
  <AddEditUserBookDialog
      v-model:visible="showUserbookDialog"
      :id-book="tempUserbook?.id"
      :is-edit="true"
      @save="submitEditUserbook"
      @cancel="showUserbookDialog = false"
  />
  <div>
    <div
        class="flex mt-5 dark:bg-surface-800 bg-surface-300 h-14 justify-center items-center gap-4"
    >
      <h2 class="text-3xl font-semibold text-primary">Moja półka - przeczytane...</h2>
      <div v-if="userbookStore.loadingUserbooks">
        <ProgressSpinner style="width: 30px; height: 30px" stroke-width="5"/>
      </div>
    </div>

    <Toolbar class="m-6 text-color">
      <template #start
      ><p class="mt-auto mb-auto">ROK: {{ displayedYear }}</p></template
      >

      <template #center>
        <InputNumber
            v-model="selectedYear"
            :min="2010"
            :max="2040"
            show-buttons
            :format="false"
            button-layout="horizontal"
        />
      </template>

      <template #end>
        <Button
            class="font-bold uppercase tracking-wider"
            outlined
            :disabled="userbookStore.loadingUserbooks"
            :loading="userbookStore.loadingUserbooks"
            @click="getUserbooks"
        >wyświetl
        </Button>
      </template>
    </Toolbar>

    <div class="flex flex-row flex-wrap justify-center">
      <div v-for="ub in userbooks" :key="ub.id">
        <UserBookSmall :userbook="ub" @edit="editUserbook"/>
      </div>
    </div>
  </div>
  <Toolbar class="sticky-toolbar m-6">
    <template #start>
      <div class="flex flex-row text-color gap-3">
        <p class="mb-1">
          <small>Audiobook:</small>
          {{ getTotalAudiobook('AUDIOBOOK') }}
        </p>
        <p class="mb-1">
          <small>Ebooki:</small>
          {{ getTotalAudiobook('EBOOK') }}
        </p>
        <p class="mb-1">
          <small>Papierowe:</small>
          {{ getTotalAudiobook('BOOK') }}
        </p>
      </div>
    </template>

    <template #end>
      <p class="mb-1 text-color">
        RAZEM:
        {{
          getTotalAudiobook('AUDIOBOOK') + getTotalAudiobook('EBOOK') + getTotalAudiobook('BOOK')
        }}
      </p>
    </template>
  </Toolbar>
</template>

<style scoped></style>
