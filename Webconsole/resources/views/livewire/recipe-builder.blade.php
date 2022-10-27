<div x-data="{ building: false, total: 0, built: 0}" @trigger-next.window="($event) => {
    let loadNext = (cache) => {
        if(cache.length > 0) {
            $wire.loadInfo(cache.pop()).then(() => {
                loadNext(cache);
            });
        }
    };
    loadNext($event.detail.cache);
}" @create-recipe.window="($event) => {
    let loadHead = (cache) => {
        built = total - cache.length;
        if(cache.length > 0) {
            $wire.loadHead(cache.pop()).then(() => {
                loadHead(cache);
            });
        }else {
            $wire.createRecipe();
        }
    }
    total = $event.detail.cache.length
    building = true;
    loadHead($event.detail.cache);
}">
    <div class="flex flex-col flex-grow p-4 bg-white rounded-md ">
        <div class="flex flex-col">
            <x-jet-input wire:model.lazy="query" type="text" class="p-4" placeholder="Search on SoftwareHeritage" />
        </div>
        <div class="px-4 sm:px-6 lg:px-8">
            <div class="flex flex-col mt-8">
                <div class="-mx-4 -my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
                    <div class="inline-block min-w-full py-2 align-middle md:px-6 lg:px-8">
                        <div class="relative overflow-hidden shadow ring-1 ring-black ring-opacity-5 md:rounded-lg">
                            <!-- Selected row actions, only show when rows are selected. -->
                            @if($phase == 0)
                            <table class="min-w-full divide-y divide-gray-300 table-fixed">
                                <thead class="bg-gray-50">
                                    <tr>
                                        <th scope="col" class="relative w-12 px-6 sm:w-16 sm:px-8">
                                            <input wire:model="selectAll" type="checkbox"
                                                class="absolute w-4 h-4 -mt-2 text-red-600 border-gray-300 rounded left-4 top-1/2 focus:ring-red-500 sm:left-6">
                                        </th>
                                        <th scope="col"
                                            class="min-w-[12rem] py-3.5 pr-3 text-left text-sm font-semibold text-gray-900 w-full">
                                            Url</th>
                                        <th scope="col" class="pr-4">
                                            <x-button wire:click="nextPhase" type="button" color="gray">Next</x-button>
                                        </th>
                                    </tr>
                                </thead>
                                <tbody class="bg-white divide-y divide-gray-200">
                                    <!-- Selected: "bg-gray-50" -->
                                    @forelse ($results as $result)
                                    <tr wire:key="result_{{ $result['crc32'] }}">
                                        <td class="relative w-12 px-6 sm:w-16 sm:px-8">
                                            <!-- Selected row marker, only show when row is selected. -->
                                            <div class="absolute inset-y-0 left-0 w-0.5 bg-red-600"></div>

                                            <input wire:model="selected.{{ $result['crc32'] }}" type="checkbox"
                                                class="absolute w-4 h-4 -mt-2 text-red-600 border-gray-300 rounded left-4 top-1/2 focus:ring-red-500 sm:left-6">
                                        </td>
                                        <!-- Selected: "text-red-600", Not Selected: "text-gray-900" -->
                                        <td class="py-4 pr-3 text-sm font-medium text-gray-900 whitespace-nowrap">{{
                                            $result['url'] }}</td>
                                        <td></td>
                                    </tr>
                                    @empty

                                    @endforelse
                                    <!-- More people... -->
                                </tbody>
                            </table>
                            @elseif($phase == 1)
                            <div class="flex flex-row items-center justify-between p-4 space-x-1" x-data="{full: true}">
                                <div class="flex flex-row space-x-2">
                                    <label
                                        class="px-4 py-1 font-semibold text-red-800 bg-red-100 border border-red-800 rounded">Retrived:
                                        {{ count($info)}} / {{ count($cache) }}</label>
                                    <label
                                        class="px-4 py-1 font-semibold text-green-800 bg-green-100 border border-green-800 rounded"
                                        x-show="building">Built: <label x-text="built"></label> / <label
                                            x-text="total"></label></label>
                                </div>
                                <div class="flex flex-row space-x-4">
                                    <div>
                                        <button wire:click="$set('full', {{ !$full }})" x-on:click="full = !full"
                                            type="button"
                                            class="relative inline-flex flex-shrink-0 h-6 mx-2 my-0.5 transition-colors duration-200 ease-in-out border-2 border-transparent rounded-full cursor-pointer w-11 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                                            role="switch" aria-checked="false"
                                            :class="{'bg-gray-200': !full, 'bg-red-600': full}">
                                            <span class="sr-only">Use setting</span>
                                            <!-- Enabled: "translate-x-5", Not Enabled: "translate-x-0" -->
                                            <span aria-hidden="true"
                                                class="inline-block w-5 h-5 transition duration-200 ease-in-out transform translate-x-0 bg-white rounded-full shadow pointer-events-none ring-0"
                                                :class="{'translate-x-0': !full, 'translate-x-5': full}"></span>
                                        </button>
                                        <label class="text-red-600">full</label>
                                    </div>
                                    <x-button class="button" color="indigo" wire:click="createRecipe">Create Recipe</x-button>
                                </div>
                            </div>
                            <div class="overflow-hidden bg-white shadow sm:rounded-md">
                                <ul role="list" class="divide-y divide-gray-200">
                                    @foreach($info as $crc32 => $repo)
                                    @if(!array_key_exists('exception', $repo))
                                    @if(($full == true && $repo['status'] == 'full') || $full == false)
                                    <li wire:key="info_{{ $crc32 }}">
                                        <a wire:click="loadBranches({{ $crc32 }})"
                                            class="block hover:bg-gray-50 {{ $repo['status'] == 'full' ? 'cursor-pointer' : 'cursor-not-allowed'}}">
                                            <div class="flex items-center px-4 py-4 sm:px-6">
                                                <div class="flex items-center flex-1 min-w-0">
                                                    <div class="flex-shrink-0">
                                                        <svg class="w-12 h-12" viewBox="0 0 256 256"
                                                            xmlns="http://www.w3.org/2000/svg"
                                                            preserveAspectRatio="xMinYMin meet">
                                                            <path
                                                                d="M251.172 116.594L139.4 4.828c-6.433-6.437-16.873-6.437-23.314 0l-23.21 23.21 29.443 29.443c6.842-2.312 14.688-.761 20.142 4.693 5.48 5.489 7.02 13.402 4.652 20.266l28.375 28.376c6.865-2.365 14.786-.835 20.269 4.657 7.663 7.66 7.663 20.075 0 27.74-7.665 7.666-20.08 7.666-27.749 0-5.764-5.77-7.188-14.235-4.27-21.336l-26.462-26.462-.003 69.637a19.82 19.82 0 0 1 5.188 3.71c7.663 7.66 7.663 20.076 0 27.747-7.665 7.662-20.086 7.662-27.74 0-7.663-7.671-7.663-20.086 0-27.746a19.654 19.654 0 0 1 6.421-4.281V94.196a19.378 19.378 0 0 1-6.421-4.281c-5.806-5.798-7.202-14.317-4.227-21.446L81.47 39.442l-76.64 76.635c-6.44 6.443-6.44 16.884 0 23.322l111.774 111.768c6.435 6.438 16.873 6.438 23.316 0l111.251-111.249c6.438-6.44 6.438-16.887 0-23.324"
                                                                fill="#DE4C36" />
                                                        </svg>
                                                    </div>
                                                    <div class="flex-1 min-w-0 px-4 md:grid md:grid-cols-2 md:gap-4">
                                                        <div>
                                                            <p class="text-sm font-medium text-red-600 truncate">{{
                                                                $repo['origin'] }}</p>
                                                            <p class="flex items-center mt-2 text-sm text-gray-500">
                                                                <!-- Heroicon name: solid/mail -->
                                                                <svg class="flex-shrink-0 mr-1.5 h-5 w-5 text-gray-400"
                                                                    xmlns="http://www.w3.org/2000/svg"
                                                                    viewBox="0 0 20 20" fill="currentColor"
                                                                    aria-hidden="true">
                                                                    <path fill-rule="evenodd"
                                                                        d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z"
                                                                        clip-rule="evenodd" />
                                                                </svg>
                                                                <span class="truncate">{{
                                                                    parse_url($repo['origin'])['host'] }}</span>
                                                            </p>
                                                        </div>
                                                        <div class="hidden md:block">
                                                            <div>
                                                                <p class="text-sm text-gray-900">
                                                                    Snapshot status:
                                                                    @switch($repo['status'])
                                                                    @case('full')
                                                                    <label class="mr-2 font-semibold text-red-600">{{
                                                                        $repo['status'] }}</label>
                                                                    @break
                                                                    @case('partial')
                                                                    <label class="mr-2 font-semibold text-red-600">{{
                                                                        $repo['status'] }}</label>
                                                                    @break
                                                                    @case('not_found')
                                                                    <label class="mr-2 font-semibold text-red-600">{{
                                                                        $repo['status'] }}</label>
                                                                    @break
                                                                    @default
                                                                    <label class="mr-2 font-semibold text-gray-600">{{
                                                                        $repo['status'] }}</label>
                                                                    @endswitch
                                                                    <time datetime="2020-01-07">{{
                                                                        $repo['date']}}</time>
                                                                </p>
                                                                <p class="flex items-center mt-2 text-sm text-gray-500">
                                                                    <!-- Heroicon name: solid/check-circle -->
                                                                    <svg class="flex-shrink-0 mr-1.5 h-5 w-5 text-green-400"
                                                                        xmlns="http://www.w3.org/2000/svg"
                                                                        viewBox="0 0 20 20" fill="currentColor"
                                                                        aria-hidden="true">
                                                                        <path fill-rule="evenodd"
                                                                            d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                                                                            clip-rule="evenodd" />
                                                                    </svg>
                                                                    Using latest snapshot
                                                                </p>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div>
                                                    <!-- Heroicon name: solid/chevron-right -->
                                                    <svg class="w-5 h-5 text-gray-400"
                                                        xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"
                                                        fill="currentColor" aria-hidden="true">
                                                        <path fill-rule="evenodd"
                                                            d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"
                                                            clip-rule="evenodd" />
                                                    </svg>
                                                </div>
                                            </div>
                                        </a>
                                        @if($selectedProject == $crc32)
                                        <div class="flex flex-col text-sm bg-gray-100 divide-y">
                                            @foreach($branches[$crc32] as $key => $branch)
                                            @if($key == 'HEAD')
                                            <div class="flex flex-row p-2 space-x-4">
                                                <label
                                                    class="px-2 font-semibold text-red-800 bg-red-100 border border-red-800 rounded">{{
                                                    $branch['target'] }}</label>
                                            </div>
                                            @else
                                            <div class="flex flex-row p-2 space-x-4">
                                                <input wire:model="projects.{{ $crc32 }}.{{ $branch['target'] }}"
                                                    type="checkbox"
                                                    class="w-4 h-4 text-red-600 border-gray-300 rounded focus:ring-red-500">
                                                <label class="font-semibold text-red-800">{{ $key }}</label>
                                            </div>
                                            @endif
                                            @endforeach
                                        </div>
                                        @endif
                                    </li>
                                    @endif
                                    @endif
                                    @endforeach
                                </ul>
                            </div>
                            <div class="flex flex-row justify-end p-2">

                            </div>
                            @else()
                            <div></div>
                            @endif
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <x-modal @rename-recipe.window="show= true" x-data="{show: false, name=''}" @close-modal.window="show = false" x-show="show"
        wire:ignore>
        <x-slot name="icon">
        </x-slot>
        <x-slot name="title">
        </x-slot>
        <x-slot name="description">
            <div class="flex flex-col space-y-4">
                <div
                    class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                    <label for="name"
                        class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">Name</label>
                    <input wire:model="name" x-model="name" type="text" name="name" id="name"
                        class="block w-full p-0 text-gray-900 placeholder-gray-500 border-0 focus:ring-0 sm:text-sm"
                        placeholder="{{ __('Recipe Name') }}">
                </div>
                <div
                    class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                    <label for="tag"
                        class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">Tag</label>
                    <input x-bind:disabled="saved" wire:model="tags" type="text" name="tag" id="tag"
                        class="block w-full p-0 text-gray-900 placeholder-gray-500 border-0 focus:ring-0 sm:text-sm"
                        placeholder="{{ __('Comma Separated Tags') }}">
                </div>
                <div
                    class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                    <label for="description"
                        class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">Description</label>
                    <input x-bind:disabled="saved" wire:model="description" type="text" name="description"
                        id="description"
                        class="block w-full p-0 text-gray-900 placeholder-gray-500 border-0 focus:ring-0 sm:text-sm"
                        placeholder="{{ __('Application description') }}">
                </div>
            </div>
        </x-slot>
        <x-slot name="action">
            <div class="flex flex-row-reverse justify-between flex-grow">
                <button x-bind:disabled="name == ''"
                    x-on:click="() => {$wire.rename().then( () => {window.location.assign('{{ route('recipe.index') }}'); name=''})}"
                    class="inline-flex items-center px-3 py-2 text-sm font-medium leading-4 text-red-700 bg-white border border-red-300 rounded-md shadow-sm hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-30">{{
                    __('Save') }}</button>
            </div>
        </x-slot>
    </x-modal>
</div>
