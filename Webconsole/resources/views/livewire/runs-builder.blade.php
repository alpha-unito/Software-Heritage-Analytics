<form x-data="{}" method="POST" action="{{ route('run.store') }}">
    @csrf
    <div class="h-full p-4 mx-auto mb-20 max-w-7xl sm:px-6 lg:px-8">
        <div class="flex flex-col h-full space-y-4">
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
                <textarea wire:model="settings" type="text" name="settings"
                    id="settings"
                    class="block w-full h-32 p-0 text-gray-900 placeholder-gray-500 border-0 focus:ring-0 sm:text-sm"
                    placeholder="{{ __('Json Settings') }}"></textarea>
            </div>
            <div
                class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                <label for="language"
                    class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">Language</label>
            <input autocomplete="off" type="hidden" wire:model="language" />
            <div @close-suggestion.window="active=false;selected=true" class="relative w-full " x-data="{ active: false, selected:false }">
                <x-jet-input type="text" autocomplete="off"  placeholder="Filter language" x-on:input='active=true;selected=false' class="w-full" name="language" wire:model='name' x-bind:class="{'border-b border-green-500': selected}" />
                <ul x-show="active" class="absolute z-10 w-full overflow-auto text-base bg-white rounded-md shadow-lg max-h-28 ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm" tabindex="-1" role="listbox" aria-labelledby="listbox-label" aria-activedescendant="listbox-option-3">
                    @foreach ($options as $key=>$language)
                    <li wire:click="setLanguage({{$key}})"class="relative py-2 pl-3 text-gray-900 cursor-default select-none pr-9 hover:bg-indigo-100" id="listbox-option-0" role="option" wire:key='{{ $language['name']."_".$key }}'>
                        <div class="flex items-center">
                            <span class="block ml-3 font-normal truncate">{{ $language['name'] }} <span class="ml-5 text-gray-500"> ({{implode(", ", $language['extensions']??[])}}) </span></span>
                        </div>
                    </li>
                    @endforeach
                    </template>
                </ul>
            </div>
            </div>
            <x-jet-input-error for="worksite_id" />
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
