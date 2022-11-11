<x-app-layout>
    <x-slot name="header">
        <div class="flex flex-row justify-between" x-data="{}">
            <h2 class="text-xl font-semibold leading-tight text-gray-800">
                {{ __('Applications') }}
            </h2>
            <a href="{{ route('run.create') }}">
                <x-button color="indigo" class="w-16">
                    <div class="w-full text-center">+</div>
                </x-button>
            </a>
        </div>
    </x-slot>
    <div class="py-12">
        <div class="mx-20 max-w-full sm:px-6 lg:px-8">
            <div class="w-full p-5 overflow-x-scroll font-mono text-white bg-black shadow-xl sm:rounded-lg">
                {!! nl2br($output) !!}
            </div>
        </div>
    </div>
</x-app-layout>
