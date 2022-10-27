<div class="relative w-full mt-1" x-data="{ disabled: {{ isset($disabled) ? $disabled : 'false' }}, active: false, selected: {{ isset($selected->id) ? $selected->id : -1 }}, label: '{{ isset($selected->name) ? $selected->name : null }}' }" x-init="() => { $watch('selected', value => $dispatch('selected-{{ $name }}', label))}">
    <button  x-bind:disabled="disabled" x-on:click="active = !active" type="button" class="relative w-full py-2 pl-3 pr-10 text-left bg-white border border-gray-300 rounded-md shadow-sm cursor-pointer focus:outline-none focus:ring-1 focus:ring-red-500 focus:border-red-500 sm:text-sm" aria-haspopup="listbox" aria-expanded="true" aria-labelledby="listbox-label">
        <span class="flex items-center">
            <span class="block ml-3 overflow-x-hidden truncate w-36" x-text="label ? label : 'Select...'"></span>
        </span>
        <span class="absolute inset-y-0 right-0 flex items-center pr-2 ml-3 pointer-events-none">
            <svg class="w-5 h-5 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path fill-rule="evenodd" d="M10 3a1 1 0 01.707.293l3 3a1 1 0 01-1.414 1.414L10 5.414 7.707 7.707a1 1 0 01-1.414-1.414l3-3A1 1 0 0110 3zm-3.707 9.293a1 1 0 011.414 0L10 14.586l2.293-2.293a1 1 0 011.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
        </span>
    </button>
    <x-jet-input type="hidden" x-model="selected" :name="$name" />
    <ul style="display: none" class="absolute z-10 w-full py-1 mt-1 overflow-auto text-base bg-white rounded-md shadow-lg max-h-56 ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm" tabindex="-1" role="listbox" aria-labelledby="listbox-label" aria-activedescendant="listbox-option-3" x-show="active">
        <li wire:key="key_empty" x-on:click="selected = -1; label = null; active = !active" class="relative py-2 pl-3 text-gray-900 cursor-pointer select-none pr-9 hover:bg-red-100" id="listbox-option-0" role="option" x-show="active">
            <div class="flex items-center">
                <span class="block ml-3 font-normal truncate">Select...</span>
            </div>
        </li>
        @foreach ($options as $option)
        <li wire:key="key_{{ $option->id }}" x-on:click='selected = {{ $option->id }}; label = "{{ $option->name }}"; active = !active' class="relative py-2 pl-3 text-gray-900 cursor-pointer select-none pr-9 hover:bg-red-100" id="listbox-option-0" role="option" x-show="active">
            <div class="flex items-center">
                <span class="block ml-3 font-normal truncate">{{ $option->name }}</span>
            </div>
            <template x-if="selected === {{ $option->id }}">
                <span class="absolute inset-y-0 right-0 flex items-center pr-4 text-red-600">
                    <x-icon-done class="w-6 h-6"></x-icon-done>
                </span>
            </template>
        </li>
        @endforeach
        </template>
    </ul>
</div>
