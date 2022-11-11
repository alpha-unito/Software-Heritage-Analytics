<x-app-layout>
    <x-slot name="header">
        <h2 class="text-xl font-semibold leading-tight text-gray-800">
            {{ __('Runs') }}
        </h2>
    </x-slot>

    <div class="py-12">
        <div class="mx-20 max-w-full sm:px-6 lg:px-8">
            <div class="overflow-hidden bg-white shadow-xl sm:rounded-lg">
                <livewire:runs-builder />
            </div>
        </div>
    </div>
</x-app-layout>
