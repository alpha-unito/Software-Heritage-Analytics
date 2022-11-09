<form method="POST" action="{{ route('run.store') }}">
    @csrf
    <div class="p-4 mx-auto mb-20 max-w-7xl sm:px-6 lg:px-8">
        <div class="flex flex-col space-y-4">
            <div
                class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                <label for="application"
                    class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">{{
                    __('Application') }}</label>
                <x-select :selected=null :options="$applications"
                name="application" ></x-select>
            </div>
            <div
                class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                <label for="recipe"
                    class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">{{
                    __('Recipe') }}</label>
                <x-select :selected=null :options="$recipes"
                    name="recipe" ></x-select>
            </div>
            <div
                class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                <label for="settings"
                    class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">Settings</label>
                <textarea x-bind:disabled="saved" wire:model="settings" x-model="settings" type="text" name="settings"
                    id="settings"
                    class="block w-full h-32 p-0 text-gray-900 placeholder-gray-500 border-0 focus:ring-0 sm:text-sm"
                    placeholder="{{ __('Json Settings') }}"></textarea>
            </div>
        </div>
        <div class="flex justify-between p-2 ">
            <a href="{{ route('run.index') }}">
                <x-button type="button" color="gray">Indietro</x-button>
            </a>
            <x-button color="indigo">Save</x-button>
        </div>
    </div>
</form>
