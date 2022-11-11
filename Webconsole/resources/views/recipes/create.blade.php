<x-app-layout>
    <x-slot name="header">
        <h2 class="text-xl font-semibold leading-tight text-gray-800">
            {{ __('Recipe') }}
        </h2>
    </x-slot>

    <div class="py-12">
        <div class="max-w-full mx-20 sm:px-6 lg:px-8">
            <div class="overflow-hidden bg-white shadow-xl sm:rounded-lg">
                <livewire:recipe-builder />
            </div>
        </div>
    </div>
</x-app-layout>
