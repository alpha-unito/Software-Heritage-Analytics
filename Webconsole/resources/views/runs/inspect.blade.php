<x-app-layout>
    <x-slot name="header">
        <div class="flex flex-row justify-between" x-data="{}">
            <h2 class="text-xl font-semibold leading-tight text-gray-800">
                {{ __('Run Output') }}
            </h2>
        </div>
    </x-slot>
    <div class="py-12">
        <div class="max-w-full mx-20 sm:px-6 lg:px-8">
            <div class="w-full p-5 overflow-x-scroll font-mono text-white bg-black shadow-xl sm:rounded-lg">
                {!! nl2br($output) !!}
            </div>
        </div>
    </div>
</x-app-layout>
